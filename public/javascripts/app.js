if (window.console) {
  console.log("Welcome to your Play application's JavaScript!");
}

var app = angular.module("clustermap", ["clustermap.map", "clustermap.searchbar"]);

app.controller("AppCtrl", function ($scope) {
});