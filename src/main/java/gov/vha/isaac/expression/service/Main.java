/*
 * Copyright 2015 kec.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.expression.service;

import gov.vha.isaac.logic.LogicGraph;
import gov.vha.isaac.logic.LogicService;
import gov.vha.isaac.lookup.constants.Constants;
import gov.vha.isaac.metadata.coordinates.EditCoordinates;
import gov.vha.isaac.metadata.coordinates.LogicCoordinates;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.metadata.coordinates.ViewCoordinates;
import gov.vha.isaac.ochre.api.IdentifierService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.TaxonomyService;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.memory.HeapUseTicker;
import gov.vha.isaac.ochre.api.progress.ActiveTasksTicker;
import gov.vha.isaac.ochre.collections.SequenceSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ihtsdo.otf.tcc.api.blueprint.ComponentProperty;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.store.TerminologySnapshotDI;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.ihtsdo.otf.tcc.api.uuid.UuidT3Generator;
import org.ihtsdo.otf.tcc.model.cc.concept.ConceptVersion;
import org.ihtsdo.otf.tcc.model.index.service.IndexerBI;
import org.ihtsdo.otf.tcc.model.index.service.SearchResult;

/**
 *
 * @author kec
 */
public class Main {

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            args = new String[]{"target"};
        }
        System.out.println("Build directory: " + args[0]);
        System.setProperty(Constants.CHRONICLE_COLLECTIONS_ROOT_LOCATION_PROPERTY, args[0] + "/data/object-chronicles");
        System.setProperty(Constants.SEARCH_ROOT_LOCATION_PROPERTY, args[0] + "/data/search");
        LookupService.startupIsaac();
        HeapUseTicker.start(10);
        ActiveTasksTicker.start(10);

        System.out.println("System up...");

        IdentifierService idService = LookupService.getService(IdentifierService.class);
        IndexerBI snomedIdLookup = LookupService.get().getService(IndexerBI.class, "snomed id refex indexer");
        IndexerBI descriptionLookup = LookupService.get().getService(IndexerBI.class, "Description indexer");
        TaxonomyService taxonomy = LookupService.getService(TaxonomyService.class);
        TerminologyStoreDI termStore = LookupService.getService(TerminologyStoreDI.class);
        LogicService logicService = LookupService.getService(LogicService.class);

        try {
            TerminologySnapshotDI statedTermSnapshot = termStore.getSnapshot(ViewCoordinates.getDevelopmentStatedLatest());
            TerminologySnapshotDI inferredTermSnapshot = termStore.getSnapshot(ViewCoordinates.getDevelopmentInferredLatest());

            UUID bleedingSnomedUuid = UuidT3Generator.fromSNOMED(131148009L);

            ConceptChronicleBI bleedingConcept1 = termStore.getConcept(bleedingSnomedUuid);
            System.out.println("\nFound [1] nid: " + bleedingConcept1.getNid());
            System.out.println("Found [1] concept sequence: " + idService.getConceptSequence(bleedingConcept1.getNid()));
            System.out.println("Found [1]: " + bleedingConcept1 + "\n " + bleedingConcept1.toLongString());
            
            LogicGraph lg1 = logicService.createLogicGraph((ConceptVersion) statedTermSnapshot.getConceptVersion(bleedingConcept1.getConceptNid()));
            System.out.println("Stated logic graph:  " + lg1);
            LogicGraph lg2 = logicService.createLogicGraph((ConceptVersion) inferredTermSnapshot.getConceptVersion(bleedingConcept1.getConceptNid()));
            System.out.println("Inferred logic graph:  " + lg2);

            List<SearchResult> bleedingSctidResult = snomedIdLookup.query("131148009", ComponentProperty.STRING_EXTENSION_1, 5);

            if (!bleedingSctidResult.isEmpty()) {
                for (SearchResult result : bleedingSctidResult) {
                    int bleedingConceptNid = result.nid;
                    System.out.println("\nFound [2] nid: " + bleedingConceptNid);
                    ConceptChronicleBI bleedingConcept2 = termStore.getConcept(bleedingConceptNid);
                    System.out.println("Found [2]: " + bleedingConcept2);
                }
            }

            List<SearchResult> bleedingDescriptionResult = descriptionLookup.query("bleeding", ComponentProperty.DESCRIPTION_TEXT, 5);
            if (!bleedingDescriptionResult.isEmpty()) {
                for (SearchResult result : bleedingDescriptionResult) {
                    int bleedingDexcriptionNid = result.nid;
                    int bleedingConceptNid = statedTermSnapshot.getConceptNidForNid(bleedingDexcriptionNid);
                    ConceptChronicleBI bleedingConcept2 = termStore.getConcept(bleedingConceptNid);
                    System.out.println("\nFound [3] nid: " + bleedingDexcriptionNid + " cNid: " + bleedingConceptNid + 
                            "; " + bleedingConcept2);
                }
            }
            Optional<LatestVersion<LogicGraph>> bleedingGraph = logicService.getLogicGraph(bleedingConcept1.getNid(),
                    LogicCoordinates.getStandardElProfile().getStatedAssemblageSequence(),
                    StampCoordinates.getDevelopmentLatest());

            if (bleedingGraph.isPresent()) {
                int sequence = logicService.getConceptSequenceForExpression(bleedingGraph.get().value(),
                        StampCoordinates.getDevelopmentLatest(),
                        LogicCoordinates.getStandardElProfile(),
                        EditCoordinates.getDefaultUserSolorOverlay());
                System.out.println("Found concept sequence "+ sequence + " for graph: " + bleedingGraph.get().value());
            } else {
                System.out.println("Found concept sequence for graph: " + bleedingGraph.get().value());
            }
            
            
            SequenceSet kindOfBleedingSequences = taxonomy.getKindOfSequenceSet(bleedingConcept1.getNid(), ViewCoordinates.getDevelopmentInferredLatest());
            System.out.println("\nHas " + kindOfBleedingSequences.size() + " kinds.");


            logicService.fullClassification(
                    StampCoordinates.getDevelopmentLatest(),
                    LogicCoordinates.getStandardElProfile(),
                    EditCoordinates.getDefaultUserSolorOverlay());



        } catch (Throwable ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        HeapUseTicker.stop();
        ActiveTasksTicker.stop();
        LookupService.shutdownIsaac();
        System.out.println("System down...");
        System.exit(0);
    }
}
