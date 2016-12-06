var app = angular.module('ratAdminApp', ['ngRoute', 'ngResource']);

app.factory('listservice', function ($resource) {
    var source = $resource('http://localhost:8080/configurations/list', {}, {
          'query': {method:'GET',isArray:true}
      });
    var data = source.query({},function(){
     console.log (data); 
    })
    return data;
});

app.config(['$routeProvider', function ($routeProvider) {
  $routeProvider
    .when("/", {templateUrl: "/html/start.html", controller: "PageCtrl"})
    .when("/list", {templateUrl: "/html/list.html", controller: "ListCtrl"})
    .otherwise("/404", {templateUrl: "/html/404.html", controller: "PageCtrl"});
}]);

app.controller('PageCtrl', function (/* $scope, $location, $http */) {
  console.log("Page Controller reporting for duty.");

});

app.controller('ListCtrl', function ($scope, listservice) {
    $scope.configurations = listservice;
});