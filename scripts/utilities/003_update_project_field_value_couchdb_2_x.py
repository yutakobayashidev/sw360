#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This is a manual database migration script. It is assumed that a
# dedicated framework for automatic migration will be written in the
# future. When that happens, this script should be refactored to conform
# to the framework's prerequisites to be run by the framework. For
# example, server address and db name should be parameterized, the code
# reorganized into a single class or function, etc.
#
# This script is for couchdb 2.x to update a field(String) in project document to a new value.
# -----------------------------------------------------------------------------

import time
import couchdb
import json
from webbrowser import get

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = True

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

couch = couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

# set values for field to be updated, OldValue and NewValue
fieldName = "field_to_be_migrated"
oldValue = "old_value"
newValue = "new_value"
isStartsWith = False

# ----------------------------------------
# queries
# ----------------------------------------

# get all the projects with oldValue
projects_all_query = {"selector": {"type": {"$eq": "project"}, fieldName: {"$eq": oldValue}}, "limit": 200000}

# get all the projects with oldValue with partial string
projects_all_query_regex = {"selector": {"type": {"$eq": "project"}, fieldName: {"$regex": oldValue}}, "limit": 200000}

# ---------------------------------------
# functions
# ---------------------------------------

def withRegex():
    log = {}
    log['updatedProjects'] = []
    print 'Getting all projects with ' + fieldName + ' starting with ' + oldValue
    projects_all_regex = db.find(projects_all_query_regex)
    print 'Received ' + str(len(projects_all_regex)) + ' projects'
    log['totalCount'] = len(projects_all_regex)

    for project in projects_all_regex:
        value = project.get(fieldName)
        if value.startswith(oldValue):
            value = value.replace(oldValue, newValue, 1)
        project[fieldName] = value
        print '\tUpdating project ID -> ' + project.get('_id') + ', Project Name -> ' + project.get('name')
        updatedProject = {}
        updatedProject['id'] = project.get('_id')
        updatedProject['name'] = project.get('name')
        log['updatedProjects'].append(updatedProject)
        if not DRY_RUN:
            db.save(project)
    resultFile = open('003_update_project_field_value_couchdb_2_x.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Total Projects with ' + fieldName + ' starts with ' + oldValue + ': ' + str(len(projects_all_regex))
    print '------------------------------------------'
    print 'Please check log file "003_update_project_field_value_couchdb_2_x.log" in this directory for details'

def withoutRegEx():
    log = {}
    log['updatedProjects'] = []
    print 'Getting all projects with ' + fieldName + ' as ' + oldValue
    projects_all = db.find(projects_all_query)
    print 'Received ' + str(len(projects_all)) + ' projects'
    log['totalCount'] = len(projects_all)

    for project in projects_all:
        project[fieldName] = newValue
        print '\tUpdating project ID -> ' + project.get('_id') + ', Project Name -> ' + project.get('name')
        updatedProject = {}
        updatedProject['id'] = project.get('_id')
        updatedProject['name'] = project.get('name')
        log['updatedProjects'].append(updatedProject)
        if not DRY_RUN:
            db.save(project)
    resultFile = open('003_update_project_field_value_couchdb_2_x.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Total Projects with ' + fieldName + ' as ' + oldValue + ': ' + str(len(projects_all))
    print '------------------------------------------'
    print 'Please check log file "003_update_project_field_value_couchdb_2_x.log" in this directory for details'
    print '------------------------------------------'

def run():
    if isStartsWith:
        withRegex()
    else:
        withoutRegEx()

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'
