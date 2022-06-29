/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/**
 * Module bridge for datatables. Sets necessary defaults and loads all dependencies as well as css files
 * needed for the default datatables in SW360.
 */
define('bridges/datatables', [
	'jquery',
	'utils/cssloader',
	/* jquery-plugins */
	'datatables.net',
	'datatables.net-bs4',
	'datatables.net-buttons',
	'datatables.net-buttons.print',
	'datatables.net-select',
	'datatables.net-select-bs4',
	'datatables-plugins-sorting.natural',
	/* own extensions */
	'modules/datatables-renderer',
	'modules/datatables-utils'
], function($, cssloader) {

	cssloader.load([
		'/webjars/datatables.net-bs4/1.10.19/css/dataTables.bootstrap4.min.css',
		'/webjars/datatables.net-select-bs4/css/select.bootstrap4.min.css'
	]);
	initialize();

	function initialize() {
		/* Set the defaults for DataTables initialisation */
		$.fn.DataTable.ext.pager.numbers_length = 8;

		$.extend( true, $.fn.dataTable.defaults, {
			// the following parameter must not be removed, otherwise it won't work anymore (probably due to datatable plugins)
			iDisplayStart: 0,
			displayStart: 0,

			autoWidth: false,
			dom:
				"<'row'<'col-auto'l><'col'B>>" + 	// line above table
				"<'row'<'col-12'tr>>" +				// table
				"<'row'<'col-auto'i><'col'p>>",		// line below table
			buttons: [],
			info: true,
			lengthMenu: [ [10, 25, 50, 100, -1], [10, 25, 50, 100, Liferay.Language.get("all")] ],
			pageLength: 10,
			paging: true,
			pagingType: 'simple_numbers',
			processing: true,
			search: { smart: false },
			searching: false,
			deferRender: true
		});
	}

	return {
		/**
		 * Creates a new datatable on the given selector.
		 * 
		 * @param {string} selector a jquery selector of the table to transform 
		 * @param {object} config configuration object for the datatable. See its documentation for details. 
		 * @param {array} printColumns array of column indexes of columns that should not be printed. Setting to undefined will not show the print button. 
		 * @param {array} noSortColumns array of column indexes of columns that shoud not be sortable
		 * @param {boolean} quickFilter flag if there should be an autosearch field on the top right corner of the table 
		 */
		create: function(selector, config, printColumns, noSortColumns, quickFilter) {
			if(typeof printColumns !== 'undefined') {
				if(!config.buttons) {
					config.buttons = [];
				}

				config.buttons.push({
					extend: 'print',
					text: '<svg class="lexicon-icon"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#print" /></svg>' +Liferay.Language.get('print'),
					autoPrint: true,
					className: 'btn btn-sm btn-secondary btn-print',
					exportOptions: {
						columns: printColumns,
						orthogonal: "print",
						stripHtml: false
					}
				});
			}

			if(typeof noSortColumns !== 'undefined') {
				if(!config.columnDefs) {
					config.columnDefs = [];
				}

				config.columnDefs.push({
					targets: noSortColumns,
					orderable: false
				});
			}

			if(quickFilter) {
				config.searching = true;

				if(typeof printColumns !== 'undefined' && printColumns.length > 0) {
                    if (config.infoOnTop) {
                        config.dom = "<'row'<'col-auto'l><'col-auto'i><'col'f><'col-auto'B>>";
                    } else {
                        config.dom = "<'row'<'col-auto'l><'col'f><'col-auto'B>>"; 	// line above table
                    }
				} else {
                    if (config.infoOnTop) {
                        config.dom = "<'row'<'col-auto'l><'col-auto'i><'col'f>>";
                    } else {
                        config.dom = "<'row'<'col-auto'l><'col'f>>";    // line above table
                    }
                }

				config.dom += '' +
					"<'row'<'col-12'tr>>" +			// table
					"<'row'<'col-auto'i><'col'p>>"; // line below table
			}

			config.drawCallback = function () {
				$('.info-text').tooltip({
					track: true,
					classes: {
						"ui-tooltip": "ui-corner-all ui-widget-shadow info-text"
					},
					content: function () {
						return $('<textarea/>').html($(this).prop('title')).val();
					},
					'data-html': 'true',
					close: function (event, ui) {
						ui.tooltip.hover(function () {
							$(this).stop(true).fadeTo(100, 1);
						},
						function () {
							$(this).fadeOut('100', function () {
								$(this).remove();
							});
						});
					}
				});
			}

			return $(selector).DataTable(config);
		},
		destroy: function(selector) {
			$(selector).DataTable({
				destroy: true
			}).destroy();
		},
		enableCheckboxForSelection: function($datatable, selectColumn) {

			// remove old listener
			$datatable.off('select.sw360.select-table');
			$datatable.off('deselect.sw360.select-table');

			// initialise selection
			$datatable.rows().deselect();
			$datatable.rows(function(idx, data, node) {
				return $(node).find(':checked').length > 0;
			}).select();

			$datatable.on('select.sw360.select-table', function(event, dataTable, type, indices) {
                var $input;

                if(type === 'row') {
					if(typeof indices.length === 'undefined') {
						indices = [ indices ];
					}

					indices.forEach(function(index) {
						$input = $(dataTable.cell(index, selectColumn).node()).find('input[type="checkbox"], input[type="radio"]');
						if($input.length > 0) {
							$input.prop('checked', true).trigger('change');
						} else {
							dataTable.row(index).deselect();
						}
					});
                }
			});

            $datatable.on('deselect.sw360.select-table', function(event, dataTable, type, indices) {
                var $input;

                if(type === 'row') {
                    if(typeof indices.length === 'undefined') {
						indices = [ indices ];
					}

					indices.forEach(function(index) {
						$input = $(dataTable.cell(index, selectColumn).node()).find('input[type="checkbox"], input[type="radio"]');
						if($input.length > 0) {
							$input.prop('checked', false).trigger('change');
						}
					});
                }
			});
		},
		showPageContainer: function($datatable) {
			$datatable = $datatable instanceof $ ? $datatable : this;

			if(!($datatable instanceof $)) {
				console.error('No jquery object found neither as parameter nor as \'this\'. Skipping.');
				return;
			}
			$datatable.parents('.container:first').show().siblings('.container-spinner').hide();
		}
	};
});
