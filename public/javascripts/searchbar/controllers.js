angular.module("clustermap.searchbar", ["clustermap.common"])
    .controller("SearchCtrl", function ($scope, moduleManager) {
        $scope.disableSearchButton = true;

        $scope.search = function () {
            if ($scope.keyword && $scope.keyword.trim().length > 0) {
                moduleManager.publishEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD,
                    {
                      keyword: $scope.keyword,
                      order: $scope.order,
                      algorithm: $scope.algorithm,
                      indexType: $scope.indexType,
                      zoom: $scope.zoom,
                      analysis: $scope.analysis,
                      treeCut: $scope.treeCut,
                      measure: $scope.measure,
                      bipartite: $scope.bipartite
                    });
            }
            else {
              moduleManager.publishEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD,
                {
                  keyword: "%",
                  order: $scope.order,
                  algorithm: $scope.algorithm,
                  indexType: $scope.indexType,
                  zoom: $scope.zoom,
                  analysis: $scope.analysis,
                  treeCut: $scope.treeCut,
                  measure: $scope.measure,
                  bipartite: $scope.bipartite
                });
            }
        };

        moduleManager.subscribeEvent(moduleManager.EVENT.WS_READY, function(e) {
            $scope.disableSearchButton = false;
        });

        $scope.orders = ["original", "reverse", "spatial", "spatial-reverse"];
        $scope.algorithms = ["SuperCluster",
          "SuperClusterInBatch", "iSuperCluster", "AiSuperCluster", "BiSuperCluster", "LBiSuperCluster", "SBiSuperCluster"];
        $scope.indexTypes = ["KDTree", "GridIndex"];
        $scope.zooms = [4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17];
        $scope.analysises = ["", "rand-index", "adjusted-rand-index"];
        $scope.treeCut = false;
        $scope.measures = ["avg", "min", "max"];
        $scope.bipartite = false;

        // Frontend mode radio
        $scope.radioFrontend = document.createElement("input");
        $scope.radioFrontend.type = "radio";
        $scope.radioFrontend.id = "frontend";
        $scope.radioFrontend.name = "mode";
        $scope.radioFrontend.value = "frontend";
        $scope.radioFrontend.style.position = 'fixed';
        $scope.radioFrontend.style.top = '110px';
        $scope.radioFrontend.style.left = '8px';
        document.body.appendChild($scope.radioFrontend);
        $scope.radioFrontend.addEventListener("click", function() {
          moduleManager.publishEvent(moduleManager.EVENT.CHANGE_MODE,
            {mode: "frontend"});
        });
        $scope.radioFrontendLabel = document.createElement("label");
        $scope.radioFrontendLabel.innerHTML = "Frontend";
        $scope.radioFrontendLabel.htmlFor ="frontend";
        $scope.radioFrontendLabel.style.position = 'fixed';
        $scope.radioFrontendLabel.style.top = '110px';
        $scope.radioFrontendLabel.style.left = '24px';
        document.body.appendChild($scope.radioFrontendLabel);

        // Middleware mode radio
        $scope.radioMiddleware = document.createElement("input");
        $scope.radioMiddleware.type = "radio";
        $scope.radioMiddleware.id = "middleware";
        $scope.radioMiddleware.name = "mode";
        $scope.radioMiddleware.value = "middleware";
        $scope.radioMiddleware.checked = true;
        $scope.radioMiddleware.style.position = 'fixed';
        $scope.radioMiddleware.style.top = '125px';
        $scope.radioMiddleware.style.left = '8px';
        document.body.appendChild($scope.radioMiddleware);
        $scope.radioMiddleware.addEventListener("click", function() {
          moduleManager.publishEvent(moduleManager.EVENT.CHANGE_MODE,
            {mode: "middleware"});
        });
        $scope.radioMiddlewareLabel = document.createElement("label");
        $scope.radioMiddlewareLabel.innerHTML = "Middleware";
        $scope.radioMiddlewareLabel.htmlFor = "middleware";
        $scope.radioMiddlewareLabel.style.position = 'fixed';
        $scope.radioMiddlewareLabel.style.top = '125px';
        $scope.radioMiddlewareLabel.style.left = '24px';
        document.body.appendChild($scope.radioMiddlewareLabel);

        // Checkbox for numbers in circles
        $scope.checkboxNumber = document.createElement("input");
        $scope.checkboxNumber.type = "checkbox";
        $scope.checkboxNumber.id = "numberInCircle";
        $scope.checkboxNumber.name = "numberInCircle";
        $scope.checkboxNumber.checked = true;
        $scope.checkboxNumber.style.position = 'fixed';
        $scope.checkboxNumber.style.top = '140px';
        $scope.checkboxNumber.style.left = '8px';
        document.body.appendChild($scope.checkboxNumber);
        $scope.checkboxNumber.addEventListener("change", function() {
          moduleManager.publishEvent(moduleManager.EVENT.CHANGE_NUMBER_IN_CIRCLE,
            {numberInCircle: this.checked});
        });
        $scope.checkboxNumberLabel = document.createElement("label");
        $scope.checkboxNumberLabel.innerHTML = "Number in Circle";
        $scope.checkboxNumberLabel.htmlFor = "numberInCircle";
        $scope.checkboxNumberLabel.style.position = 'fixed';
        $scope.checkboxNumberLabel.style.top = '140px';
        $scope.checkboxNumberLabel.style.left = '24px';
        document.body.appendChild($scope.checkboxNumberLabel);
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
                '<label for="order">Order</label>&nbsp;<select id="order" ng-model="order" ng-options="x for x in orders" ng-init="order = orders[0]"></select>&nbsp;',
                '<label for="algorithm">Algorithm</label>&nbsp;<select id="algorithm" ng-model="algorithm" ng-options="x for x in algorithms" ng-init="algorithm = algorithms[0]"></select>&nbsp;',
                '<label for="indexType">IndexType</label>&nbsp;<select id="indexType" ng-model="indexType" ng-options="x for x in indexTypes" ng-init="indexType = indexTypes[0]"></select>&nbsp;',
                '<label for="zoom">Zoom</label>&nbsp;<select id="zoom" ng-model="zoom" ng-options="x for x in zooms" ng-init="zoom = zooms[0]"></select>&nbsp;',
                '<label for="analysis">Analysis</label>&nbsp;<select id="analysis" ng-model="analysis" ng-options="x for x in analysises" ng-init="analysis = analysises[0]"></select>&nbsp;',
                '<label for="treeCut">Tree-Cut</label>&nbsp;<input id="treeCut" type="checkbox" ng-model="treeCut"></input>&nbsp;',
                '<label for="measure">Measure</label>&nbsp;<select id="measure" ng-model="measure" ng-options="x for x in measures" ng-init="measure = measures[0]" ng-change="search()"></select>&nbsp;',
                '<label for="bipartite">Bipartite</label>&nbsp;<input id="bipartite" type="checkbox" ng-model="bipartite"></input>&nbsp;'
            ].join('')
        };
    });