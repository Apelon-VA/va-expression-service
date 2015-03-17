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

import gov.vha.isaac.lookup.constants.Constants;
import gov.vha.isaac.metadata.coordinates.ViewCoordinates;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.TaxonomyProvider;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.collections.SequenceSet;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ihtsdo.otf.tcc.api.blueprint.ComponentProperty;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.store.TerminologySnapshotDI;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.ihtsdo.otf.tcc.api.uuid.UuidT3Generator;
import org.ihtsdo.otf.tcc.model.cc.termstore.TerminologySnapshot;
import org.ihtsdo.otf.tcc.model.index.service.IndexerBI;
import org.ihtsdo.otf.tcc.model.index.service.SearchResult;

/**
 *
 * @author kec
 */
public class Main {
    public static void main(String [] args) {
        System.out.println("Hello world");
        System.out.println("Build directory: " + args[0]);
        System.setProperty(Constants.CHRONICLE_COLLECTIONS_ROOT_LOCATION_PROPERTY, args[0] + "/data/object-chronicles");
        System.setProperty(Constants.SEARCH_ROOT_LOCATION_PROPERTY, args[0] + "/data/search");

        LookupService.getRunLevelController().proceedTo(2);
        System.out.println("System up...");
        
        IndexerBI snomedIdLookup = LookupService.get().getService(IndexerBI.class, "snomed id refex indexer");
        TaxonomyProvider taxonomy = LookupService.getService(TaxonomyProvider.class);
        TerminologyStoreDI termStore = LookupService.getService(TerminologyStoreDI.class);
        try {
            TerminologySnapshotDI termSnapshot = termStore.getSnapshot(ViewCoordinates.getDevelopmentInferredLatest());
            
            UUID bleedingSnomedUuid = UuidT3Generator.fromSNOMED(131148009L);
            
            ConceptVersionBI bleedingConcept1 = termSnapshot.getConceptVersion(bleedingSnomedUuid);
            System.out.println("Found [1]: " + bleedingConcept1);
            SequenceSet kindOfBleedingSequences = taxonomy.getKindOfSequenceSet(bleedingConcept1.getNid(), ViewCoordinates.getDevelopmentInferredLatest());
            System.out.println("Has " + kindOfBleedingSequences.size() + " kinds.");
            
            List<SearchResult> bleedingSctidResult = snomedIdLookup.query("131148009", ComponentProperty.LONG_EXTENSION_1, 1);
            // Sorry, refex search still needs some work. 
            if (!bleedingSctidResult.isEmpty()) {
                int bleedingNid = bleedingSctidResult.get(0).nid;
                ConceptVersionBI bleedingConcept2 = termSnapshot.getConceptVersion(bleedingNid);
                System.out.println("Found [2]: " + bleedingConcept2);
            }
            
            
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        LookupService.getRunLevelController().proceedTo(-1);
        System.out.println("System down...");
        System.exit(0);
    }
}
