package tests.generatorTests;

import tests.TestSeries;
import vnreal.io.XMLExporter;
import vnreal.network.NetworkStack;

public class AdvSubgraphMain2 {

        /**
         * @param args
         */
        public static void main(String[] args) {
                AdvSubgraphTestGenerator2 gen = new AdvSubgraphTestGenerator2();
                
                TestSeries series = gen.generateTests();
                
                XMLExporter exporter = new XMLExporter(AdvSubgraphMain2.class.getSimpleName(), series.getTestSeriesName(), series.getTestGenerator(), false);
                
                AdvSubgraphTestRunner2 run = new AdvSubgraphTestRunner2(exporter);
                //run.prepareRunnerStage2(series.getAllTestRuns().get(0));
                //NetworkStack stack = run.getNetworkStack();
                
                run.runAllTest(series.getAllTestRuns(), 1);
                
                
                //XMLExporter.exportResult("C:/Dokumente und Einstellungen/kokot/Desktop/Test_gen10.xml", series, false);
                
                //System.out.println(series.getAllTestRuns().size());
                
                
//              TestSeries series2 = XMLImporter.importResults("C:/Dokumente und Einstellungen/kokot/Desktop/Test_gen10.xml");
//              
//              System.out.println(series2.getAllTestRuns().size());

        }

}
