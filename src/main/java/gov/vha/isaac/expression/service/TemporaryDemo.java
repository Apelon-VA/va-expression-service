package gov.vha.isaac.expression.service;
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

import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.And;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.ConceptAssertion;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.NecessarySet;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.SomeRole;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.SufficientSet;
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.metadata.coordinates.EditCoordinates;
import gov.vha.isaac.metadata.coordinates.LogicCoordinates;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.metadata.coordinates.TaxonomyCoordinates;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.ConceptModel;
import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.IdentifierService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.TaxonomyService;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.classifier.ClassifierResults;
import gov.vha.isaac.ochre.api.classifier.ClassifierService;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.commit.CommitService;
import gov.vha.isaac.ochre.api.component.concept.ConceptBuilder;
import gov.vha.isaac.ochre.api.component.concept.ConceptBuilderService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptService;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilder;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.constants.Constants;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.coordinate.LogicCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.index.IndexServiceBI;
import gov.vha.isaac.ochre.api.index.SearchResult;
import gov.vha.isaac.ochre.api.logic.LogicService;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilderService;
import gov.vha.isaac.ochre.api.memory.HeapUseTicker;
import gov.vha.isaac.ochre.api.progress.ActiveTasksTicker;
import gov.vha.isaac.ochre.api.relationship.RelationshipVersionAdaptor;
import gov.vha.isaac.ochre.collections.SequenceSet;
import gov.vha.isaac.ochre.model.logic.LogicalExpressionOchreImpl;
import gov.vha.isaac.ochre.util.UuidT3Generator;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;


/**
 *
 * @author kec
 */
public class TemporaryDemo {

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            File temp = new File("target/data/");
            args = new String[]{temp.listFiles()[0].getAbsolutePath()};
        }
        System.out.println("Build directory: " + args[0]);
        System.setProperty(Constants.DATA_STORE_ROOT_LOCATION_PROPERTY, args[0]);
    //System.setProperty(Constants.CHRONICLE_COLLECTIONS_ROOT_LOCATION_PROPERTY, args[0] + "/object-chronicles");
        //System.setProperty(Constants.SEARCH_ROOT_LOCATION_PROPERTY, args[0] + "/search");
        LookupService.getService(ConfigurationService.class)
                .setConceptModel(ConceptModel.OCHRE_CONCEPT_MODEL);
        LookupService.startupIsaac();
        HeapUseTicker.start(10);
        ActiveTasksTicker.start(10);

        System.out.println("System up...");

        LogicCoordinate logicCoordinate = LogicCoordinates.getStandardElProfile();
        StampCoordinate stampCoordinate = StampCoordinates.getDevelopmentLatest();
        EditCoordinate editCoordinate = EditCoordinates.getDefaultUserSolorOverlay();

        ConceptService conceptService = Get.conceptService();

        IdentifierService idService = Get.identifierService();
        IndexServiceBI descriptionLookup = LookupService.get().getService(IndexServiceBI.class, "description indexer");
        TaxonomyService taxonomy = LookupService.getService(TaxonomyService.class);
        LogicService logicService = LookupService.getService(LogicService.class);
        ClassifierService classifierService = logicService.getClassifierService(stampCoordinate,
                logicCoordinate, editCoordinate);
        CommitService commitService = LookupService.getService(CommitService.class);

        try {

            UUID bleedingSnomedUuid = UuidT3Generator.fromSNOMED(131148009L);

            ConceptChronology bleedingConcept1 = conceptService.getConcept(bleedingSnomedUuid);
            System.out.println("\nFound [1] nid: " + bleedingConcept1.getNid());
            System.out.println("Found [1] concept sequence: " + idService.getConceptSequence(bleedingConcept1.getNid()));
            System.out.println("Found [1]: " + bleedingConcept1 + "\n " + bleedingConcept1);

            Optional<LatestVersion<? extends LogicalExpression>> lg1 = logicService.getLogicalExpression(bleedingConcept1.getNid(), logicCoordinate.getStatedAssemblageSequence(), stampCoordinate);
            System.out.println("Stated logic graph:  " + lg1);
            Optional<LatestVersion<? extends LogicalExpression>> lg2 = logicService.getLogicalExpression(bleedingConcept1.getNid(), logicCoordinate.getInferredAssemblageSequence(), stampCoordinate);
            System.out.println("Inferred logic graph:  " + lg2);

            Optional<Integer> nid = OchreUtility.getNidForSCTID(131148009L);

            if (nid.isPresent()) {
                System.out.println("\nFound [2] nid via index: " + nid.get());
                ConceptChronology bleedingConcept2 = conceptService.getConcept(nid.get());
                    System.out.println("Found [2]: " + bleedingConcept2);
                }
            else
            {
                System.err.println("Failed to find via index!");
            }

            List<SearchResult> bleedingDescriptionResult = descriptionLookup.query("bleeding", 5);
            if (!bleedingDescriptionResult.isEmpty()) {
                for (SearchResult result : bleedingDescriptionResult) {
                    int bleedingDexcriptionNid = result.nid;
                    int bleedingConceptNid = idService.getConceptNidForDescriptionNid(bleedingDexcriptionNid);
                    ConceptChronology bleedingConcept2 = conceptService.getConcept(bleedingConceptNid);
                    System.out.println("\nFound [3] nid: " + bleedingDexcriptionNid + " cNid: " + bleedingConceptNid
                            + "; " + bleedingConcept2);
                }
            }
            Optional<LatestVersion<? extends LogicalExpression>> bleedingGraph = logicService.getLogicalExpression(bleedingConcept1.getNid(),
                    LogicCoordinates.getStandardElProfile().getStatedAssemblageSequence(),
                    StampCoordinates.getDevelopmentLatest());

            if (bleedingGraph.isPresent()) {
                int sequence = classifierService.getConceptSequenceForExpression(bleedingGraph.get().value(),
                        editCoordinate).get();
                System.out.println("Found concept sequence " + sequence + " for graph: " + bleedingGraph.get().value());
            } else {
                System.out.println("No concept sequence for graph: " + bleedingGraph);
            }
            ClassifierResults results = classifierService.classify().get();

            LogicalExpressionBuilderService expressionBuilderService = LookupService.getService(LogicalExpressionBuilderService.class);
            LogicalExpressionBuilder defBuilder = expressionBuilderService.getLogicalExpressionBuilder();

            SufficientSet(And(ConceptAssertion(Get.conceptService().getConcept(Snomed.BLEEDING_FINDING.getNid()), defBuilder),
                    SomeRole(conceptService.getConcept(Snomed.FINDING_SITE.getNid()),
                            ConceptAssertion(conceptService.getConcept(Snomed.ABDOMINAL_WALL_STRUCTURE.getNid()), defBuilder))));

            LogicalExpression abdominalWallBleedingDef = defBuilder.build();

            System.out.println("Created definition:\n\n " + abdominalWallBleedingDef);
            
            //============================================================================================================================================
            //
            //  LOINC NAME:  Sodium:SCnc:Pt:Ser/Plas:Qn 
            //  LOINC CODE:  2951-2
            //
            //  Expression from OWL version of first technology preview as rendered in Protege then formatted manually to clarify structure;
            //  Note that SNOMED FSNs sometimes differ from names in the OWL technology preview shown here:
            //
            //      'observable entity'
            //      and 
            //      ('Specified by' some ('observation procedure'
            //                            and 
            //                            (Scale some Qn)
            //                            and 
            //                            ('Direct site' some Ser/Plas)
            //                            and 
            //                            (Observes some ('Feature of entity'
            //                                            and 
            //                                            ('Property type' some SCnc)
            //                                            and
            //                                            ('Inheres in' some Plasma)
            //                                            and
            //                                            (Towards some Sodium)))))
            //          
            //  Hard-wired code to construct corresponding LogicalExpression in ISAAC ...
                      
            defBuilder = expressionBuilderService.getLogicalExpressionBuilder();
            
            long SPECIFIED_BY = 704346009l;
            long OBSERVATION_PROCEDURE = 1;  //TODO missing
            long SCALE = 27811002L;
            long QN = 1;  //TODO missing
            long DIRECT_SITE = 704327008L;
            long SER_OR_PLAS = 1; //TODO missing
            long OBSERVES = 704347000L;
            long FEATURE_OF_ENTITY = 414237002L;
            long PROPERTY_TYPE = 704318007;
            long SCNC = 118556004L;
            long INHERES_IN = 704319004L;
            long PLASMA = 50863008L;
            long TOWARDS = 70432005L;
            long SODIUM = 39972003L;
            
            
            SufficientSet(
                    And(ConceptAssertion(Get.conceptService().getConcept(Snomed.OBSERVABLE_ENTITY.getNid()), defBuilder),
                            SomeRole(conceptService.getConcept(OchreUtility.getNidForSCTID(SPECIFIED_BY).get()),
                                    And(ConceptAssertion(Get.conceptService().getConcept(OchreUtility.getNidForSCTID(OBSERVATION_PROCEDURE).get()), defBuilder),        
                                            SomeRole(conceptService.getConcept(OchreUtility.getNidForSCTID(SCALE).get()),
                                                    ConceptAssertion(conceptService.getConcept(OchreUtility.getNidForSCTID(QN).get()), defBuilder)),        
                                            SomeRole(conceptService.getConcept(OchreUtility.getNidForSCTID(DIRECT_SITE).get()),
                                                    ConceptAssertion(conceptService.getConcept(OchreUtility.getNidForSCTID(SER_OR_PLAS).get()), defBuilder)),
                                            SomeRole(conceptService.getConcept(OchreUtility.getNidForSCTID(OBSERVES).get()),
                                                    And(ConceptAssertion(Get.conceptService().getConcept(OchreUtility.getNidForSCTID(FEATURE_OF_ENTITY).get()), defBuilder),
                                                            SomeRole(conceptService.getConcept(OchreUtility.getNidForSCTID(PROPERTY_TYPE).get()),
                                                                    ConceptAssertion(conceptService.getConcept(OchreUtility.getNidForSCTID(SCNC).get()), defBuilder)),
                                                            SomeRole(conceptService.getConcept(OchreUtility.getNidForSCTID(INHERES_IN).get()),
                                                                    ConceptAssertion(conceptService.getConcept(OchreUtility.getNidForSCTID(PLASMA).get()), defBuilder)),    
                                                            SomeRole(conceptService.getConcept(OchreUtility.getNidForSCTID(TOWARDS).get()),
                                                                    ConceptAssertion(conceptService.getConcept(OchreUtility.getNidForSCTID(SODIUM).get()), defBuilder))))))));    

            LogicalExpression sampleLoincExpression = defBuilder.build();
            
            System.out.println("Created sample LOINC expression:\n\n " + sampleLoincExpression);

            //  NOTES:
            //                   
            //  -  Each unresolved constant needs a suitable constant ConceptSpec created in type Snomed.         
            //  -  Every corresponding concept must be suitably defined in ISAAC (within SNOMED or an extension thereof).
            //  -  Concepts for each of the attributes are indeed present in the July 2015 version of SNOMED (didn't check January 2015).
            //  -  Likewise, each of the focus concepts are present; note that "Evaluation procedure (procedure)" is used for observation procedure
            //  -  The preceding code is untested since ISAAC is not currently operational.  
            //  -  Hopefully, toString() creates useful results and the println invocation will confirm correct results.
            //
            //============================================================================================================================================

            int newSequence = classifierService.getConceptSequenceForExpression((LogicalExpressionOchreImpl) abdominalWallBleedingDef,
                    editCoordinate).get();

            ClassifierResults results2 = classifierService.classify().get();

            SequenceSet kindOfBleedingSequences = taxonomy.getKindOfSequenceSet(bleedingConcept1.getNid(), TaxonomyCoordinates
                    .getStatedTaxonomyCoordinate(StampCoordinates.getDevelopmentLatestActiveOnly(), Get.configurationService().getDefaultLanguageCoordinate()));
            System.out.println("\nHas " + kindOfBleedingSequences.size() + " stated kinds.");

            if (kindOfBleedingSequences.contains(newSequence)) {
                System.out.println("Stated Kind-of set includes new concept " + newSequence);
            } else {
                System.out.println("Error: Stated kind-of set does not include new concept " + newSequence);
            }

            kindOfBleedingSequences = taxonomy.getKindOfSequenceSet(bleedingConcept1.getNid(), TaxonomyCoordinates
                    .getInferredTaxonomyCoordinate(StampCoordinates.getDevelopmentLatestActiveOnly(), Get.configurationService().getDefaultLanguageCoordinate()));
            System.out.println("\nHas " + kindOfBleedingSequences.size() + " inferred kinds.");

            if (kindOfBleedingSequences.contains(newSequence)) {
                System.out.println("Inferred Kind-of set includes new concept " + newSequence);
            } else {
                System.out.println("Error: Inferred kind-of set does not include new concept " + newSequence);
            }

            System.out.println("Test rels from root");
            StringBuilder sb = new StringBuilder();
            ConceptChronology root = conceptService.getConcept(IsaacMetadataAuxiliaryBinding.ISAAC_ROOT.getNid());
            Collection<? extends RelationshipVersionAdaptor> incomingRels = root.getRelationshipListOriginatingFromConcept();
            AtomicInteger relCount = new AtomicInteger(1);
            if (incomingRels.isEmpty()) {
                System.out.println(" No incoming rels for: " + root);
            } else {
                System.out.println(" Found " + incomingRels.size() + " incoming rels for: " + root);
                incomingRels.forEach((rel) -> {
                    sb.append(relCount.getAndIncrement()).append(": ").append(rel).append("\n");
                });

            }
            System.out.println(sb.toString());

        } catch (Throwable ex) {
            Logger.getLogger(TemporaryDemo.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            // BEGIN test 
            {
                // Add new concept and definition here to classify. 
                System.out.println("Testing creation and save of nested aspects");
                ConceptBuilderService conceptBuilderService = LookupService.getService(ConceptBuilderService.class);
                conceptBuilderService.setDefaultLanguageForDescriptions(IsaacMetadataAuxiliaryBinding.ENGLISH);
                conceptBuilderService.setDefaultDialectAssemblageForDescriptions(IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT);
                conceptBuilderService.setDefaultLogicCoordinate(LogicCoordinates.getStandardElProfile());

                DescriptionBuilderService descriptionBuilderService = LookupService.getService(DescriptionBuilderService.class);
                LogicalExpressionBuilderService expressionBuilderService
                        = LookupService.getService(LogicalExpressionBuilderService.class);
                LogicalExpressionBuilder defBuilder = expressionBuilderService.getLogicalExpressionBuilder();

                NecessarySet(And(ConceptAssertion(conceptService.getConcept(Snomed.ORGANISM.getConceptSequence()), defBuilder)));

                LogicalExpression def = defBuilder.build();
                System.out.println("Created definition:\n\n " + def);

                String testName = "BogusName";
                String refexFSN = testName + " FSN";
                String refexPreferredTerm = testName + " PT";

                ConceptBuilder builder = conceptBuilderService.getDefaultConceptBuilder(
                        refexFSN, refexPreferredTerm, def);

                String refexDescription = "Description for " + testName;

                DescriptionBuilder definitionBuilder = descriptionBuilderService.
                        getDescriptionBuilder(refexDescription, builder,
                                IsaacMetadataAuxiliaryBinding.DEFINITION_DESCRIPTION_TYPE,
                                IsaacMetadataAuxiliaryBinding.ENGLISH);

                definitionBuilder.setPreferredInDialectAssemblage(IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT);
                builder.addDescription(definitionBuilder);

                List createdComponents = new ArrayList();

                //Build this on the lowest level path, otherwise, other code that references this will fail (as it doesn't know about custom paths)
                System.out.println("Constructing " + testName + " concept...");

                ConceptChronology newCon = builder.build(EditCoordinates.getDefaultUserSolorOverlay(), ChangeCheckerMode.ACTIVE, createdComponents);

                for (Object component : createdComponents) {
                    component.toString();
                }

                System.out.println("Adding uncommitted...");
                commitService.addUncommitted(newCon);

                System.out.println("Committing...");
                commitService.commit("Commit for logic integration incremental classification test. ").get();

                ClassifierResults results = classifierService.classify().get();
                System.out.println(results);

                System.out.println("Constructed concept " + newCon.getLatestVersion(ConceptVersion.class, stampCoordinate).get());

                System.out.println("Displaying concept description refexes");
                newCon.getConceptDescriptionList().forEach((desc) -> {
                    System.out.println("Displaying concept description " + desc + " annotations");
                    SememeChronology<? extends DescriptionSememe> description
                            = (SememeChronology<? extends DescriptionSememe>) desc;
                    int numSememes = 0;
                    for (SememeChronology<? extends SememeVersion> annotation : description.getSememeList()) {
                        numSememes++;
                        System.out.println("Desc " + desc + " has sememe " + annotation);
                    }
                    System.out.println("Desc " + desc + " has " + numSememes + " sememes");
                });

            }
            // END test

        } catch (Throwable ex) {
            Logger.getLogger(TemporaryDemo.class.getName()).log(Level.SEVERE, null, ex);
        }

        HeapUseTicker.stop();
        ActiveTasksTicker.stop();
        LookupService.shutdownIsaac();
        System.out.println("System down...");
        System.exit(0);
    }
}

