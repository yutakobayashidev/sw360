<table class="table spdx-table" id="editDocumentCreationInformation">
  <thead>
    <tr>
      <th colspan="3">
        <liferay-ui:message key="2.document.creation.information" />
      </th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td style="display: flex">
        <div class="form-group" style="flex: 1">
          <label class="mandatory" for="spdxVersion">2.1 SPDX Version</label>
          <div style="display: flex">
            <label class="sub-label">SPDX-</label>
            <input id="spdxVersion" class="form-control needs-validation" rule="isNotNull"
              type="text" placeholder="Enter SPDX Version"
              value="${spdxDocumentCreationInfo.spdxVersion}">
          </div>
          <div id="spdxVersion-error-messages">
            <div class="invalid-feedback" rule="isNotNull">
              <liferay-ui:message key="this.field.must.be.not.empty" />
            </div>
          </div>
        </div>
        <div class="form-group" style="flex: 1">
          <label class="mandatory" for="dataLicense">2.2 Data License</label>
          <input id="dataLicense" class="form-control needs-validation" rule="isNotNull" type="text"
            placeholder="<liferay-ui:message key="enter.data.license" />"
            value="<sw360:out value="${spdxDocumentCreationInfo.dataLicense}" />">
          <div id="dataLicense-error-messages">
            <div class="invalid-feedback" rule="isNotNull">
              <liferay-ui:message key="this.field.must.be.not.empty" />
            </div>
          </div>
        </div>
        <div class="form-group" style="flex: 1">
          <label class="mandatory" for="spdxIdentifier">2.3 SPDX Identifier</label>
          <div style="display: flex">
            <label class="sub-label">SPDXRef-</label>
            <input id="spdxIdentifier" class="form-control needs-validation" rule="isNotNull" type="text"
              placeholder="Enter SPDX Identifier" value="${spdxDocumentCreationInfo.SPDXID}">
          </div>
          <div id="spdxIdentifier-error-messages">
            <div class="invalid-feedback" rule="isNotNull">
              <liferay-ui:message key="this.field.must.be.not.empty" />
            </div>
          </div>
        </div>
      </td>
    </tr>
    <tr>
      <td>
        <div class="form-group">
          <label class="mandatory" for="documentName">2.4. Document Name</label>
          <input id="documentName" type="text"
            class="form-control needs-validation" rule="isNotNull"
            placeholder="<liferay-ui:message key="enter.spdx.document.name" />" value="${spdxDocumentCreationInfo.name}">
        </div>
        <div id="documentName-error-messages">
          <div class="invalid-feedback" rule="isNotNull">
            <liferay-ui:message key="this.field.must.be.not.empty" />
          </div>
        </div>
      </td>
    </tr>
    <tr>
      <td>
        <div class="form-group">
          <label class="mandatory" for="documentNamespace">2.5 SPDX Document Namespace</label>
          <input id="documentNamespace" class="form-control needs-validation" rule="isNotNull" type="text"
            placeholder="<liferay-ui:message key=" enter.spdx.document.namespace" />"
            value="${spdxDocumentCreationInfo.documentNamespace}">
        </div>
        <div id="documentNamespace-error-messages">
          <div class="invalid-feedback" rule="isNotNull">
            <liferay-ui:message key="this.field.must.be.not.empty" />
          </div>
        </div>
      </td>
    </tr>
    <tr>
      <td class="spdx-full">
        <div class="form-group section section-external-doc-ref">
          <label for="externalDocumentRefs">2.6 External Document References</label>
          <div style="display: flex; flex-direction: column; padding-left: 1rem;">
            <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
              <label for="externalDocumentRefs" style="text-decoration: underline;"
                class="sub-title">Select Reference</label>
              <select id="externalDocumentRefs" type="text" class="form-control spdx-select">
              </select>
              <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-externalDocRef"
                data-row-id="" viewBox="0 0 512 512">
                <title><liferay-ui:message key="delete" /></title>
                <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
              </svg>
            </div>
            <button class="spdx-add-button-main" name="add-externalDocRef">Add new Reference</button>
          </div>
          <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
            <label class="sub-title" for="externalDocumentId">External Document ID</label>
            <input id="externalDocumentId" style="width: auto; flex: auto;" type="text" class="form-control"
              placeholder="Enter External Document ID">
          </div>
          <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
            <label class="sub-title" for="externalDocument">External Document</label>
            <input id="externalDocument" style="width: auto; flex: auto;" type="text" class="form-control"
              placeholder="Enter External Document">
          </div>
          <div style="display: flex;">
            <label class="sub-title">Checksum</label>
            <div style="display: flex; flex-direction: column; flex: 7">
              <div style="display: flex; margin-bottom: 0.75rem;">
                <input style="flex: 2; margin-right: 1rem;" type="text" class="form-control"
                  id="checksumAlgorithm" placeholder="Enter Algorithm">
                <input style="flex: 6;" type="text" class="form-control" id="checksumValue"
                  placeholder="Enter Value">
              </div>
            </div>
          </div>
        </div>
      </td>
    </tr>
    <tr class="spdx-full">
      <td>
        <div class="form-group">
          <label for="licenseListVersion">2.7 License List Version</label>
          <input id="licenseListVersion" class="form-control" type="text"
            placeholder="Enter License List Version" value="${spdxDocumentCreationInfo.licenseListVersion}">
        </div>
      </td>
    </tr>
    <tr>
      <td>
        <div class="form-group">
          <label class="mandatory" for="creator">2.8 Creator </label>
          <div style="display: flex; flex-direction: column;">
            <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
              <label class="sub-title" for="creator-anonymous">Anonymous</label>
              <input id="creator-anonymous" class="spdx-checkbox" type="checkbox" onclick="setAnonymous()" >
            </div>
            <div style="display: flex;">
              <label class="sub-title">List</label>
              <div style="display: flex; flex-direction: column; flex: 7">
                <div style="display: none; margin-bottom: 0.75rem;" name="creatorRow">
                  <select style="flex: 2; margin-right: 1rem;" type="text"
                    class="form-control creator-type" placeholder="Enter Type"
                    onchange="changeCreatorType(this)">
                    <option value="Organization" selected>Organization</option>
                    <option value="Person">Person</option>
                    <option value="Tool">Tool</option>
                  </select>
                  <input style="flex: 6; margin-right: 2rem;" type="text"
                    class="form-control creator-value" placeholder="Enter Value">
                  <svg class="disabled lexicon-icon spdx-delete-icon-sub"
                    name="delete-spdx-creator" data-row-id="" viewBox="0 0 512 512">
                    <title><liferay-ui:message key="delete" /></title>
                    <path class="lexicon-icon-outline lx-trash-body-border" d="M64.4,440.7c0,39.3,31.9,71.3,71.3,71.3h240.6c39.3,0,71.3-31.9,71.3-71.3v-312H64.4V440.7z M128.2,192.6h255.5v231.7c0,13.1-10.7,23.8-23.8,23.8H152c-13.1,0-23.8-10.7-23.8-23.8V192.6z"></path>
                    <polygon class="lexicon-icon-outline lx-trash-lid" points="351.8,32.9 351.8,0 160.2,0 160.2,32.9 64.4,32.9 64.4,96.1 447.6,96.1 447.6,32.9 "></polygon>
                    <rect class="lexicon-icon-outline lx-trash-line-2" x="287.9" y="223.6" width="63.9" height="191.6"></rect>
                    <rect class="lexicon-icon-outline lx-trash-line-1" x="160.2" y="223.6" width="63.9" height="191.6"></rect>
                  </svg>
                </div>
                <button class="spdx-add-button-sub spdx-add-button-sub-creator" name="add-spdx-creator">Add new creator</button>
              </div>
            </div>
          </div>
        </div>
      </td>
    </tr>
    <tr>
      <td style="display: flex; flex-direction: column;">
        <div class="form-group">
          <label class="mandatory" for="createdDate">2.9 Created</label>
          <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
            <input id="createdDate" type="date" class="form-control spdx-date needs-validation"
              rule="isNotNull" placeholder="created.date.yyyy.mm.dd">
            <input id="createdTime" type="time" step="1" class="form-control spdx-time needs-validation"
              rule="isNotNull" placeholder="created.time.hh.mm.ss">
          </div>
          <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
            <div id="createdDate-error-messages">
              <div class="invalid-feedback" rule="isNotNull">
                <liferay-ui:message key="this.field.must.be.not.empty" />
              </div>
            </div>
            <div id="createdTime-error-messages">
              <div class="invalid-feedback" rule="isNotNull">
                <liferay-ui:message key="this.field.must.be.not.empty" />
              </div>
            </div>
          </div>
        </div>
      </td>
    </tr>
  </tbody>
</table>

<script>
  function setAnonymous() {
    let selectboxes = $('#creator-anonymous').parent().next().find('select');
    if ($('#creator-anonymous').is(':checked')) {
      selectboxes.each(function (index) {
        if ($(this).val() == 'Organization' || $(this).val() == 'Person') {
          $(this).attr('disabled', 'true');
          $(this).next().attr('disabled', 'true');
          $(this).next().next().css('cursor', 'not-allowed');
        }
      });
    } else {
      selectboxes.each(function (index) {
        if ($(this).val() == 'Organization' || $(this).val() == 'Person') {
          $(this).removeAttr('disabled');
          $(this).next().removeAttr('disabled');
          $(this).next().next().css('cursor', 'pointer');
        }
      });
    }
  }

  function changeCreatorType(selectbox) {
    if ($('#creator-anonymous').is(':checked') &&
      ($(selectbox).val() == 'Organization' || $(selectbox).val() == 'Person')) {
      $(selectbox).attr('disabled', 'true');
      $(selectbox).next().attr('disabled', 'true');
      $(selectbox).next().next().css('cursor', 'not-allowed');
    }
  }

</script>