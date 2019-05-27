angular.module('bonitasoft.ui.extensions',['ngSanitize'])
 .filter('labelized', [function () {
   return function toType(state) {
     return "<span class=\"label label-"+severity(state)+"\">"+state+"</span>";
   };
}]).filter('fromNow', [function (format) {
   return function fromNow(input) {
 	return moment(input,format).fromNow();
   };
}]);

function severity(status){
    switch(status) {
     case "Discretionary": return "warning";
     case "completed": return "success";
     case "error": return "danger";
     case "Completed": return "success";
     case "Required": return "danger";
     case "Optional": return "primary";
     default:
       return "default";
   }
}