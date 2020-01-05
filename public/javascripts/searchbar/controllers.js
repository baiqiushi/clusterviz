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
                      pixels: $scope.pixels,
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
                  pixels: $scope.pixels,
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
        $scope.measures = ["max", "min", "avg"];
        $scope.pixelsOptions = [1, 2, 3, 4, 5, 10, 15, 20];
        $scope.bipartite = false;
        $scope.mwVisualizationTypes = ["scatter", "heat"];

        /** Left side controls */
        //Zoom Shift Select
        $scope.selectZoomShift = document.createElement("select");
        $scope.selectZoomShift.title = "zoomShift";
        $scope.selectZoomShift.style.position = 'fixed';
        $scope.selectZoomShift.style.top = '90px';
        $scope.selectZoomShift.style.left = '8px';
        for (let i = 0; i <= 6; i ++) {
          let option = document.createElement("option");
          option.text = ""+ i;
          $scope.selectZoomShift.add(option);
        }
        $scope.selectZoomShift.value = "0";
        document.body.appendChild($scope.selectZoomShift);
        $scope.selectZoomShift.addEventListener("change", function () {
          moduleManager.publishEvent(moduleManager.EVENT.CHANGE_ZOOM_SHIFT,
            {zoomShift: $scope.selectZoomShift.value});
        });
        $scope.selectZoomShiftLabel = document.createElement("label");
        $scope.selectZoomShiftLabel.innerHTML = "Zoom Shift";
        $scope.selectZoomShiftLabel.htmlFor ="zoomShift";
        $scope.selectZoomShiftLabel.style.position = 'fixed';
        $scope.selectZoomShiftLabel.style.top = '90px';
        $scope.selectZoomShiftLabel.style.left = '45px';
        document.body.appendChild($scope.selectZoomShiftLabel);

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

        // Select for Visualization Types under Middleware mode
        $scope.addSelectMWVisualizationTypes = function() {
          // Select for middleware mode visualization types
          $scope.selectMWVisualizationTypes = document.createElement("select");
          $scope.selectMWVisualizationTypes.id = "mwVisualizationTypes";
          $scope.selectMWVisualizationTypes.title = "mwVisualizationTypes";
          $scope.selectMWVisualizationTypes.style.position = 'fixed';
          $scope.selectMWVisualizationTypes.style.top = '125px';
          $scope.selectMWVisualizationTypes.style.left = '105px';
          for (let i = 0; i < $scope.mwVisualizationTypes.length; i ++) {
            let option = document.createElement("option");
            option.text = $scope.mwVisualizationTypes[i];
            $scope.selectMWVisualizationTypes.add(option);
          }
          $scope.selectMWVisualizationTypes.value = $scope.mwVisualizationTypes[0];
          document.body.appendChild($scope.selectMWVisualizationTypes);
          $scope.selectMWVisualizationTypes.addEventListener("change", function () {
            moduleManager.publishEvent(moduleManager.EVENT.CHANGE_MW_VISUALIZATION_TYPE,
              {mwVisualizationType: $scope.selectMWVisualizationTypes.value});
          });
        };
        // by default show it, since by default mode = "middleware";
        $scope.addSelectMWVisualizationTypes();
        // only show it when mode is "middleware"
        moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_MODE, function(e) {
          if (e.mode === "middleware") {
            // if select element does not exist, create it
            if (document.getElementById("mwVisualizationTypes") === null) {
              $scope.addSelectMWVisualizationTypes();
            }
          }
          else {
            // if select element exists, remove it
            if (document.getElementById("mwVisualizationTypes")) {
              document.body.removeChild($scope.selectMWVisualizationTypes);
              $scope.selectMWVisualizationTypes = null;
            }
          }
        });

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

        // Checkbox for color encoding
        $scope.checkboxColor = document.createElement("input");
        $scope.checkboxColor.type = "checkbox";
        $scope.checkboxColor.id = "colorEncoding";
        $scope.checkboxColor.name = "colorEncoding";
        $scope.checkboxColor.checked = true;
        $scope.checkboxColor.style.position = 'fixed';
        $scope.checkboxColor.style.top = '155px';
        $scope.checkboxColor.style.left = '8px';
        document.body.appendChild($scope.checkboxColor);
        $scope.checkboxColor.addEventListener("change", function() {
          moduleManager.publishEvent(moduleManager.EVENT.CHANGE_COLOR_ENCODING,
            {colorEncoding: this.checked});
        });
        $scope.checkboxColorLabel = document.createElement("label");
        $scope.checkboxColorLabel.innerHTML = "Color Encoding";
        $scope.checkboxColorLabel.htmlFor = "colorEncoding";
        $scope.checkboxColorLabel.style.position = 'fixed';
        $scope.checkboxColorLabel.style.top = '155px';
        $scope.checkboxColorLabel.style.left = '24px';
        document.body.appendChild($scope.checkboxColorLabel);

        // Circle Radius Select
        $scope.selectCircleRadius = document.createElement("select");
        $scope.selectCircleRadius.title = "circleRadius";
        $scope.selectCircleRadius.style.position = 'fixed';
        $scope.selectCircleRadius.style.top = '175px';
        $scope.selectCircleRadius.style.left = '8px';
        for (let i = 20; i >=5; i -= 5) {
          let option = document.createElement("option");
          option.text = ""+ i;
          $scope.selectCircleRadius.add(option);
        }
        for (let i = 4; i >=1 ; i --) {
          let option = document.createElement("option");
          option.text = ""+ i;
          $scope.selectCircleRadius.add(option);
        }
        $scope.selectCircleRadius.value = "20";
        document.body.appendChild($scope.selectCircleRadius);
        $scope.selectCircleRadius.addEventListener("change", function () {
          moduleManager.publishEvent(moduleManager.EVENT.CHANGE_CIRCLE_RADIUS,
            {circleRadius: $scope.selectCircleRadius.value});
        });
        $scope.selectCircleRadiusLabel = document.createElement("label");
        $scope.selectCircleRadiusLabel.innerHTML = "Circle Radius";
        $scope.selectCircleRadiusLabel.htmlFor ="circleRadius";
        $scope.selectCircleRadiusLabel.style.position = 'fixed';
        $scope.selectCircleRadiusLabel.style.top = '175px';
        $scope.selectCircleRadiusLabel.style.left = '50px';
        document.body.appendChild($scope.selectCircleRadiusLabel);

        // Checkbox for scale circle radius
        $scope.checkboxScaleCircleRadius = document.createElement("input");
        $scope.checkboxScaleCircleRadius.type = "checkbox";
        $scope.checkboxScaleCircleRadius.id = "scaleCircleRadius";
        $scope.checkboxScaleCircleRadius.name = "scaleCircleRadius";
        $scope.checkboxScaleCircleRadius.checked = false;
        $scope.checkboxScaleCircleRadius.style.position = 'fixed';
        $scope.checkboxScaleCircleRadius.style.top = '195px';
        $scope.checkboxScaleCircleRadius.style.left = '8px';
        document.body.appendChild($scope.checkboxScaleCircleRadius);
        $scope.checkboxScaleCircleRadius.addEventListener("change", function() {
          moduleManager.publishEvent(moduleManager.EVENT.CHANGE_SCALE_CIRCLE_RADIUS,
            {scaleCircleRadius: this.checked});
        });
        $scope.checkboxScaleCircleRadiusLabel = document.createElement("label");
        $scope.checkboxScaleCircleRadiusLabel.innerHTML = "Scale Circle Radius";
        $scope.checkboxScaleCircleRadiusLabel.htmlFor = "scaleCircleRadius";
        $scope.checkboxScaleCircleRadiusLabel.style.position = 'fixed';
        $scope.checkboxScaleCircleRadiusLabel.style.top = '195px';
        $scope.checkboxScaleCircleRadiusLabel.style.left = '24px';
        document.body.appendChild($scope.checkboxScaleCircleRadiusLabel);
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
                '<label for="algorithm">Algorithm</label>&nbsp;<select id="algorithm" ng-model="algorithm" ng-options="x for x in algorithms" ng-init="algorithm = algorithms[5]"></select>&nbsp;',
                '<label for="indexType">IndexType</label>&nbsp;<select id="indexType" ng-model="indexType" ng-options="x for x in indexTypes" ng-init="indexType = indexTypes[0]"></select>&nbsp;',
                '<label for="zoom">Zoom</label>&nbsp;<select id="zoom" ng-model="zoom" ng-options="x for x in zooms" ng-init="zoom = zooms[0]"></select>&nbsp;',
                '<label for="analysis">Analysis</label>&nbsp;<select id="analysis" ng-model="analysis" ng-options="x for x in analysises" ng-init="analysis = analysises[0]"></select>&nbsp;',
                '<label for="treeCut">Tree-Cut</label>&nbsp;<input id="treeCut" type="checkbox" ng-model="treeCut"></input>&nbsp;',
                '<label for="measure">Measure</label>&nbsp;<select id="measure" ng-model="measure" ng-options="x for x in measures" ng-init="measure = measures[0]"></select>&nbsp;',
                '<label for="pixels">Pixels</label>&nbsp;<select id="pixels" ng-model="pixels" ng-options="x for x in pixelsOptions" ng-init="pixels = pixelsOptions[1]"></select>&nbsp;',
                '<label for="bipartite">Bipartite</label>&nbsp;<input id="bipartite" type="checkbox" ng-model="bipartite"></input>&nbsp;'
            ].join('')
        };
    });