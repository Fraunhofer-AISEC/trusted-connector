var app = angular.module('ratAdminApp', ['ngRoute', 'ngResource']);

app.factory('listservice', function ($resource) {
    return{
        query: function(id) {
          return $resource('http://localhost:31337/configurations/list', {}, {
             query: { method: 'GET', isArray: true }
          }).query();
        }
    }
});

app.factory('editservice',function($resource){
  return{
    query: function(id) {
      return $resource('http://localhost:31337/configurations/'+id, {}, {
             query: { method: 'GET', isArray: false }
      }).query();
    },
    delete: function(id) {
      $resource('http://localhost:31337/configurations/delete/'+id, {}, {
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
  console.log("Page Controller reporting in for duty.");

});

app.controller('ListCtrl', function ($scope, $rootScope, $location, listservice, editservice) {
    $scope.configurations = listservice.query();
    $scope.deleteConfig = function ( id ) {
        editservice.delete(id);
        $scope.configurations = listservice.query();
    };    
});

app.controller('EditCtrl', function ($scope, $location, $routeParams, editservice) {
    $scope.id = $routeParams.id;
    $scope.configuration = editservice.query($scope.id);
    $scope.types = ['BASIC','ADVANCED','ALL'];
    $scope.go = function ( path ) {
        $location.path( path );
    };
});
