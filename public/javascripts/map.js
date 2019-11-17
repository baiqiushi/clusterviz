angular.module("clustermap.map", ["leaflet-directive", "clustermap.common"])
  .controller("MapCtrl", function($scope, $timeout, leafletData, moduleManager) {

    $scope.zoomShift = 0;

    $scope.query = {
      cluster: "",
      order: "",
      algorithm: "",
      zoom: 0,
      bbox: []
    };

    $scope.ws = new WebSocket("ws://" + location.host + "/ws");
    $scope.pinmapMapResul = [];

    $scope.sendQuery = function(e) {
      console.log("e = " + JSON.stringify(e));

      if (e.keyword) {
        $scope.query.keyword = e.keyword;
      }

      if (e.order) {
        $scope.query.order = e.order;
      }

      if ($scope.map) {
        $scope.query.zoom = $scope.map.getZoom() + $scope.zoomShift;
        $scope.query.bbox = [
          $scope.map.getBounds().getWest(),
          $scope.map.getBounds().getSouth(),
          $scope.map.getBounds().getEast(),
          $scope.map.getBounds().getNorth()
        ];
      }

      if (e.algorithm) {
        $scope.query.algorithm = e.algorithm;
      }

      // only send query when comprised query has enough information, i.e. keyword, order, algorithm
      if ($scope.query.keyword && $scope.query.order && $scope.query.algorithm) {
        $scope.query.cluster = $scope.query.keyword + "-" + $scope.query.order + "-" + $scope.query.algorithm;

        let request = {
          type: "query",
          id: $scope.query.keyword,
          keyword: $scope.query.keyword,
          query: $scope.query
        };

        // by default, comparing the given algorithm with SuperCluster
        if (request.query.algorithm.toLowerCase() !== "supercluster") {
          request.analysis = {
            objective: "randindex",
            arguments: [
              request.query.keyword + "-" + request.query.order + "-SuperCluster",
              request.query.cluster,
              request.query.zoom
            ]
          };
        }

        console.log("sending query:");
        console.log(JSON.stringify(request));

        $scope.ws.send(JSON.stringify(request));

        document.getElementById("myBar").style.width = "0%";
      }
    };

    $scope.sendCmd = function(id, keyword, commands) {
      let cmd = {
        type: "cmd",
        id: id,
        keyword: keyword,
        cmds: commands
      };

      console.log("sending cmd:");
      console.log(JSON.stringify(cmd));

      $scope.ws.send(JSON.stringify(cmd));
    };

    $scope.waitForWS = function() {

      if ($scope.ws.readyState !== $scope.ws.OPEN) {
        window.setTimeout($scope.waitForWS, 1000);
      }
      else {
        moduleManager.publishEvent(moduleManager.EVENT.WS_READY, {});

        moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, function(e) {
          $scope.cleanClusterMap();
          $scope.sendQuery(e);
        });

        moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, function(e) {
          $scope.sendQuery(e);
        });

        moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_SHIFT, function(e) {
          $scope.zoomShift = parseInt(e.zoomShift);
          $scope.sendQuery(e);
        });

        moduleManager.subscribeEvent(moduleManager.EVENT.CONSOLE_INPUT, function(e) {
          console.log("sending console command:");
          console.log(JSON.stringify(e));
          $scope.ws.send(JSON.stringify(e));
        });

        $scope.map.on('moveend', function() {
          moduleManager.publishEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, {});
        });
      }
    };

    setting default map styles, zoom level, etc.
    angular.extend($scope, {
      tiles: {
        name: 'Mapbox',
        url: 'https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}',
        type: 'xyz',
        options: {
          accessToken: 'pk.eyJ1IjoiamVyZW15bGkiLCJhIjoiY2lrZ2U4MWI4MDA4bHVjajc1am1weTM2aSJ9.JHiBmawEKGsn3jiRK_d0Gw',
          id: 'jeremyli.p6f712pj'
        }
      },
      controls: {
        custom: []
      }
    });
    // angular.extend($scope, {
    //   tiles: {
    //     name: 'OpenStreetMap',
    //     url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    //     attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
    //   },
    //   controls: {
    //     custom: []
    //   }
    // });

    // initialize the leaflet map
    $scope.init = function() {
      leafletData.getMap().then(function (map) {
        $scope.map = map;
        $scope.bounds = map.getBounds();
        //making attribution control to false to remove the default leaflet sign in the bottom of map
        map.attributionControl.setPrefix(false);
        map.setView([$scope.lat, $scope.lng], $scope.zoom);
      });

      //Reset Zoom Button
      var button = document.createElement("a");
      var text = document.createTextNode("Reset");
      button.appendChild(text);
      button.title = "Reset";
      button.href = "#";
      button.style.position = 'fixed';
      button.style.top = '70px';
      button.style.left = '8px';
      button.style.fontSize = '14px';
      var body = document.body;
      body.appendChild(button);
      button.addEventListener("click", function () {
        $scope.map.setView([$scope.lat, $scope.lng], 4);
      });

      //Zoom Shift Select
      $scope.selectZoomShift = document.createElement("select");
      $scope.selectZoomShift.title = "zoomShift";
      $scope.selectZoomShift.style.position = 'fixed';
      $scope.selectZoomShift.style.top = '90px';
      $scope.selectZoomShift.style.left = '8px';
      for (let i = 0; i <= 10; i ++) {
        let option = document.createElement("option");
        option.text = ""+ i;
        $scope.selectZoomShift.add(option);
      }
      body = document.body;
      body.appendChild($scope.selectZoomShift);
      $scope.selectZoomShift.addEventListener("change", function () {
        moduleManager.publishEvent(moduleManager.EVENT.CHANGE_ZOOM_SHIFT,
          {zoomShift: $scope.selectZoomShift.value});
      });

      $scope.waitForWS();
    };

    $scope.handleResult = function(result) {
      if(result.data.length > 0) {
        let resultCount = result.data.length;
        let pointsCount = 0;
        for (let i = 0; i < resultCount; i ++) {
          pointsCount += (result.data[i].properties.point_count===0?1:result.data[i].properties.point_count);
        }
        moduleManager.publishEvent(moduleManager.EVENT.CHANGE_RESULT_COUNT, {resultCount: resultCount, pointsCount: pointsCount});
        $scope.drawClusterMap(result.data);
      }
    };

    $scope.ws.onmessage = function(event) {
      $timeout(function() {
        const response = JSON.parse(event.data);

        console.log("===== websocket response =====");
        console.log(JSON.stringify(response));

        if (response.type === "query") {
          $scope.handleResult(response.result);
          if (typeof response.progress == "number") {
            document.getElementById("myBar").style.width = response.progress + "%";
          }
        }
        else {
          if (response.id === "console") {
            moduleManager.publishEvent(moduleManager.EVENT.CONSOLE_OUTPUT, response);
          }
        }
      });
    };

    // function for drawing clustermap
    $scope.drawClusterMap = function(data) {

      // initialize the points layer
      if (!$scope.pointsLayer) {
        $scope.pointsLayer = L.geoJson(null, { pointToLayer: $scope.createClusterIcon}).addTo($scope.map);
        $scope.points = [];
      }

      // update the points layer
      if (data.length > 0) {
        $scope.points = data;
        console.log("drawing points size = " + $scope.points.length);
        //console.log($scope.points);
        $scope.pointsLayer.clearLayers();
        $scope.pointsLayer.addData($scope.points);
      }

      // analysis of distance between clicked points
      $scope.pointsLayer.on('contextmenu', (e) => {
        if (e.layer.feature.properties.id) {
          if ($scope.selectedCount !== 1) {
            $scope.p1 = e.layer.feature.properties.id;
            console.log("point1 id[" + $scope.p1 + "] selected!");
            $scope.selectedCount = 1;
          }
          else if ($scope.selectedCount === 1 && e.layer.feature.properties.id !== $scope.p1) {
            $scope.p2 = e.layer.feature.properties.id;
            console.log("point2 id[" + $scope.p2 + "] selected!");
            $scope.selectedCount = 2;
          }
          if ($scope.selectedCount === 2) {
            let analysis = {
              type: "analysis",
              keyword: $scope.keyword,
              analysis: {
                objective: "distance",
                arguments: [
                  $scope.keyword + "-" + $scope.order, // clusterKey
                  $scope.map.getZoom() + 1, // zoom
                  $scope.p1, // point1_id
                  $scope.p2
                ]
              }
            };
            console.log("sending analysis:");
            console.log(JSON.stringify(analysis));
            $scope.ws.send(JSON.stringify(analysis));
          }
        }
      });

      $scope.pointsLayer.on('click', (e) => {
        // zoom in if click on a cluster
        if (e.layer.feature.properties.point_count) {
          $scope.map.flyTo(e.latlng, e.layer.feature.properties.expansionZoom);
        }
      });

      return 0;
    };

    $scope.createClusterIcon = function(feature, latlng) {
      if (feature.properties.point_count === 0) return L.circleMarker(latlng, {radius: 2, fillColor: 'blue'});

      const count = feature.properties.point_count;
      const size =
        count < 20 ? 'point' :
          count < 100 ? 'small' :
            count < 1000 ? 'medium' : 'large';
      //const iconSize = Math.max(10, count / 100);
      const icon = L.divIcon({
        html: `<div><span>${feature.properties.point_count_abbreviated}</span></div>`,
        className: `marker-cluster marker-cluster-${size}`,
        iconSize: L.point(40, 40)
        //iconSize: L.point(iconSize, iconSize)
      });

      return L.marker(latlng, {icon: icon, title: feature.properties.id, alt: feature.properties.id});
    };

    $scope.cleanClusterMap = function() {
      $scope.points = [];
      if($scope.pointsLayer != null) {
        $scope.pointsLayer.clearLayers();
        $scope.map.removeLayer($scope.pointsLayer);
        $scope.pointsLayer = null;
      }
    };
  })
  .directive("map", function () {
    return {
      restrict: 'E',
      scope: {
        lat: "=",
        lng: "=",
        zoom: "="
      },
      controller: 'MapCtrl',
      template:[
        '<leaflet lf-center="center" tiles="tiles" events="events" controls="controls" width="100%" height="100%" ng-init="init()"></leaflet>'
      ].join('')
    };
  });


angular.module("clustermap.map")
  .controller('CountCtrl', function($scope, moduleManager) {
    $scope.resultCount = "";

    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_RESULT_COUNT, function(e) {
      $scope.resultCount = e.resultCount + ": " + e.pointsCount;
    })
  });