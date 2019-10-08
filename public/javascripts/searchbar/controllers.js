angular.module("clustermap.searchbar", ["clustermap.common"])
    .controller("SearchCtrl", function ($scope, moduleManager) {
        $scope.disableSearchButton = true;

        $scope.search = function () {
            if ($scope.keyword && $scope.keyword.trim().length > 0) {
                //Splits out all individual words in the query keyword.
                var keywords = $scope.keyword.trim().split(/\s+/);

                if (keywords.length === 0) {
                    alert("Your query only contains stopwords. Please re-enter your query.");
                } else {
                    moduleManager.publishEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD,
                        {keyword: keywords[0], order: $scope.order, progressive: $scope.progressive, zoom: $scope.zoom});
                }
            }
        };

        moduleManager.subscribeEvent(moduleManager.EVENT.WS_READY, function(e) {
            $scope.disableSearchButton = false;
        });

        $scope.orders = ["original", "reverse", "spatial", "spatial-reverse"];
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
                '<select ng-model="order" ng-options="x for x in orders" ng-init="order = orders[0]"></select>',
                '<input id="progressive" type="checkbox" ng-model="progressive" ng-init="progressive = false"/><label for="progressive">Progressive</label>',
                '<select ng-model="zoom" ng-options="x for x in zooms" ng-init="zoom = zooms[0]"></select>'
            ].join('')
        };
    });
