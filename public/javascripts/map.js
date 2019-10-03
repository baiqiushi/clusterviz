angular.module("clustermap.map", ["leaflet-directive", "clustermap.common"])
  .controller("MapCtrl", function($scope, $timeout, leafletData, moduleManager) {

    $scope.times = [];
    $scope.timestamps = {};

    $scope.keyword = "";
    $scope.resultCount = 0;

    $scope.ws = new WebSocket("ws://" + location.host + "/ws");
    $scope.pinmapMapResul = [];

    $scope.sendQuery = function(e) {
      console.log("e = " + e);

      $scope.resultCount = 0;

      if (e.keyword) {
        $scope.keyword = e.keyword;
      }

      let query = {
        type: "query",
        id: $scope.keyword,
        keyword: $scope.keyword,
        query: {
          cluster: "a",
          zoom: $scope.map.getZoom(),
          bbox: [$scope.map.getBounds().getWest(),
            $scope.map.getBounds().getSouth(),
            $scope.map.getBounds().getEast(),
            $scope.map.getBounds().getNorth()]
        }
      };

      $scope.timestamps.t0 = Date.now();
      console.log("sending query:");
      console.log(JSON.stringify(query));

      $scope.ws.send(JSON.stringify(query));
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
        let commands = [{action: "load"}, {action: "cluster", arguments: ["a", "original"]}];
        $scope.sendCmd("init", "wildfire", commands);
      }
    };

    // setting default map styles, zoom level, etc.
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
      button.style.position = 'inherit';
      button.style.top = '10%';
      button.style.left = '1%';
      button.style.fontSize = '14px';
      var body = document.body;
      body.appendChild(button);
      button.addEventListener("click", function () {
        $scope.map.setView([$scope.lat, $scope.lng], 4);
      });

      $scope.waitForWS();
      // moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_SEARCH_KEYWORD, function(e) {
      //   $scope.cleanClusterMap();
      //   $scope.sendQuery(e);
      // });
    };

    $scope.handleResult = function(result) {
      if(result.data.length > 0) {
        $scope.resultCount += result.data.length;
        moduleManager.publishEvent(moduleManager.EVENT.CHANGE_RESULT_COUNT, {resultCount: $scope.resultCount});
        $scope.drawClusterMap(result.data);
      }
    };

    $scope.ws.onmessage = function(event) {
      $timeout(function() {
        const response = JSON.parse(event.data);

        console.log(JSON.stringify(response));

        if (response.type === "cmd") {
          if (response.id === "init") {
            if (response.result.cursor === "cluster" && response.status === "done") {
              let e = {keyword: "wildfire"};
              $scope.sendQuery(e);

              $scope.map.on('moveend', function() {
                $scope.sendQuery({keyword: "wildfire"});
              });
            }
          }
        }
        else if (response.type === "analysis") {
        }
        else {
          $scope.handleResult(response.result);
        }
      });
    };

    // function for drawing clustermap
    $scope.drawClusterMap = function(data) {

      // initialize the points layer
      if (!$scope.pointsLayer) {
        $scope.pointsLayer = L.geoJson(null, { pointToLayer: $scope.createClusterIcon,
          onEachFeature: function (feature, layer) {
            layer.bindPopup('<h1>'+feature.properties.id+'</h1><p>points: '+feature.properties.point_count+'</p>');
          }}).addTo($scope.map);
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

      $scope.pointsLayer.on('click', (e) => {
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
                cluster: "a",
                zoom: $scope.map.getZoom() + 1,
                p1: $scope.p1,
                p2: $scope.p2
              }
            };
            console.log("sending analysis:");
            console.log(JSON.stringify(analysis));
            $scope.ws.send(JSON.stringify(analysis));
          }
        }
      });

      return 0;
    };

    $scope.createClusterIcon = function(feature, latlng) {
      if (feature.properties.point_count === 0) return L.marker(latlng);

      const count = feature.properties.point_count;
      const size =
        count < 100 ? 'small' :
          count < 1000 ? 'medium' : 'large';
      const icon = L.divIcon({
        html: `<div><span>${  feature.properties.point_count_abbreviated  }</span></div>`,
        className: `marker-cluster marker-cluster-${  size}`,
        iconSize: L.point(40, 40)
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
    $scope.resultCount = 0;

    moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_RESULT_COUNT, function(e) {
      $scope.resultCount = e.resultCount;
    })
  });