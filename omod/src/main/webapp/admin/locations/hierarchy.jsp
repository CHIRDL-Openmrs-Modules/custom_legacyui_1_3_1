<%@ include file="/WEB-INF/view/module/legacyui/template/include.jsp" %>

<openmrs:require privilege="Manage Location Tags" otherwise="/login.htm" redirect="/admin/locations/locationTag.list" />

<%@ include file="/WEB-INF/view/module/legacyui/template/header.jsp" %>
<%@ include file="localHeader.jsp" %>

<openmrs:htmlInclude file="/scripts/jquery/jquery.min.js" /> 
<openmrs:htmlInclude file="/scripts/jquery/jsTree/jstree.min.js" /> 
<openmrs:htmlInclude file="/scripts/jquery/jsTree/themes/default/style.css" />


<script type="text/javascript">

  $j(document).ready(function() {

    $j('#hierarchyTree').jstree({

       core: {
        data: ${json}     
      },

      plugins : [ "sort", "state" ]  

     }).on('loaded.jstree', function() {
             $j('#hierarchyTree') .jstree('open_all');

     }).on('click', '.jstree-anchor', function (e) { //toggle node with single click
            $j(this).jstree(true).toggle_node(e.target);
     })
        
  });


</script>



<fieldset style="clear: both">
    <legend><openmrs:message code="Location.hierarchy"/></legend>
    <div id="hierarchyTree"></div>
</fieldset>

<openmrs:extensionPoint pointId="org.openmrs.admin.locations.hierarchy.footer" type="html" />

<%@ include file="/WEB-INF/view/module/legacyui/template/footer.jsp" %>
