angular.module("clustermap.searchbar", ["clustermap.common"])
    .controller("SearchCtrl", function ($scope, moduleManager) {
        $scope.disableSearchButton = true;

        $scope.search = function () {
            if ($scope.keyword && $scope.keyword.trim().length > 0) {
                moduleManager.publishEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD,
                    {keyword: $scope.keyword, order: $scope.order, algorithm: $scope.algorithm, zoom: $scope.zoom});
            }
            else {
              moduleManager.publishEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD,
                {keyword: "%", order: $scope.order, algorithm: $scope.algorithm, zoom: $scope.zoom});
            }
        };

        moduleManager.subscribeEvent(moduleManager.EVENT.WS_READY, function(e) {
            $scope.disableSearchButton = false;
        });

        $scope.orders = ["original", "reverse", "spatial", "spatial-reverse"];
        $scope.algorithms = ["SuperCluster", "SuperClusterInBatch", "iSuperCluster"];
        $scope.zooms = [4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18];
    })
    .directive("searchBar", function () {
        return {
            restrict: "E",
            controller: "SearchCtrl",
            template: [
                '<form class="form-inline" id="input-form" ng-submit="search()" >',
                '  <div class="input-group col-lg-12">',
                '    <label class="sr-only">Keywords</label>',
                '    <input type="text" style="width: 97%" class="form-control " id="keyword-textbox" placeholder="Search keywords, e.g. hurricane" ng-model="keyword"/>',
                '    <span class="input-group-btn">',
                '      <button type="submit" class="btn btn-primary" id="submit-button" ng-disabled="disableSearchButton">Submit</button>',
                '    </span>',
                '  </div>',
                '  <div id="myProgress" class="input-group col-lg-12" style="width: 69%">',
                '    <div id="myBar"></div>',
                '  </div>',
                '</form>',
                '<label for="order">Order</label><select id="order" ng-model="order" ng-options="x for x in orders" ng-init="order = orders[0]"></select>',
                '<label for="algorithm">Algorithm</label><select id="algorithm" ng-model="algorithm" ng-options="x for x in algorithms" ng-init="algorithm = algorithms[0]"></select>',
                '<label for="zoom">Zoom</label><select id="zoom" ng-model="zoom" ng-options="x for x in zooms" ng-init="zoom = zooms[0]"></select>'
            ].join('')
        };
    });
