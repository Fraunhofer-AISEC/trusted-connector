var app = angular.module('ratAdminApp', ['ngRoute', 'ngResource']);

app.factory('listservice', function ($resource) {
    var source = $resource('http://localhost:31330/configurations/list', {}, {
          'query': {method:'GET',isArray:true}
      });
    var data = source.query({},function(){
     //console.log (data); 
    })
    return data;
});

app.factory('editservice',function($resource){
  return{
  query: function(id) {
      return $resource('http://localhost:31330/configurations/'+id, {}, {
             query: { method: 'GET', isArray: false }
      }).query();
    }
  }
});

app.config(['$routeProvider', function ($routeProvider) {
  $routeProvider
    .when("/", {templateUrl: "/html/start.html", controller: "PageCtrl"})
    .when("/list", {templateUrl: "/html/list.html", controller: "ListCtrl"})
    .when("/edit/:id", {templateUrl: "/html/edit.html", controller: "EditCtrl"})
    .otherwise("/404", {templateUrl: "/html/404.html", controller: "PageCtrl"});
}]);

app.controller('PageCtrl', function (/* $scope, $location, $http */) {
  console.log("Page Controller reporting for duty.");

});

app.controller('ListCtrl', function ($scope, listservice) {
    $scope.configurations = listservice;
});

app.controller('EditCtrl', function ($scope, $routeParams, editservice) {
    $scope.id = $routeParams.id;
    $scope.configuration = editservice.query($scope.id);
	$scope.types = ['BASIC','ADVANCED','ALL'];
});