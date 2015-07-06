function updateLightGraph(lightData) {
    renderLightChart(lightData);
}


function renderLightChart(chartDataRaw){
    var chartWrapperElmId = "#canvas-wrapper2";
    var graphWidth = $(chartWrapperElmId).width() - 50;
    if (chartDataRaw.length == 0) {
        $(chartWrapperElmId).html("No data available...");
        return;
    }

    var chartData = [[], []];
    for (var i = 0; i < chartDataRaw.length; i++){
        chartData[0].push({x:parseInt(chartDataRaw[i].x), y:parseInt(chartDataRaw[i].y)});
        chartData[1].push({x:parseInt(chartDataRaw[i].x), y:Math.abs(parseInt(chartDataRaw[i].y) - 1)});
    }

    //var i = parseInt(fromDate);
    //while (i < parseInt(toDate)) {
    //    var rnd = Math.round(Math.random());
    //    chartData[0].push({x: i * 1000, y: rnd});
    //    chartData[1].push({x: i * 1000, y: Math.abs(rnd - 1)});
    //    i += 60 * 5;
    //}

    var chartDiv = "chart2";
    var sliderDiv = "slider2";
    var x_axis = "x_axis2";
    var y_axis = "y_axis2";
    $(chartWrapperElmId).html("").html('<div id="' + y_axis + '" class="custom_y_axis"></div><div id="' + chartDiv + '" class="custom_rickshaw_graph"></div><div id="' + x_axis + '" class="custom_x_axis"></div><div id="' + sliderDiv + '" class="custom_slider"></div>');

    var graph = new Rickshaw.Graph({
        element: document.getElementById(chartDiv),
        width: graphWidth,
        height: 150,
        strokeWidth: 0.5,
        renderer: 'bar_no_gap',
        xScale: d3.time.scale(),
        padding: {top: 0.2, left: 0.02, right: 0.02, bottom: 0},
        series: [
            {color: 'steelblue', data: chartData[0]},
            {color: 'white', data: chartData[1]}
        ]
    });

    graph.registerRenderer(new Rickshaw.Graph.Renderer.BarNoGap({graph: graph}));

    graph.render();

    var xAxis = new Rickshaw.Graph.Axis.X({
        graph: graph,
        orientation: 'bottom',
        element: document.getElementById(x_axis),
        tickFormat: graph.x.tickFormat()
    });

    xAxis.render();

    var yAxis = new Rickshaw.Graph.Axis.Y({
        graph: graph,
        orientation: 'left',
        element: document.getElementById(y_axis),
        width: 40,
        height: 160,
        tickFormat: function (y) {
            switch (y) {
                case 1:
                    return 'ON';
                case 0:
                    return 'OFF';
                default :
                    return '';
            }
        }
    });

    yAxis.render();

    var slider = new Rickshaw.Graph.RangeSlider.Preview({
        graph: graph,
        element: document.getElementById(sliderDiv)
    });
}