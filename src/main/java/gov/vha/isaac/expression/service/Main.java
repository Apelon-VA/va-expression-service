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

import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.And;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.ConceptAssertion;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.SomeRole;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.SufficientSet;
import gov.vha.isaac.logic.LogicGraph;
import gov.vha.isaac.logic.LogicService;
import gov.vha.isaac.metadata.coordinates.EditCoordinates;
import gov.vha.isaac.metadata.coordinates.LogicCoordinates;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.metadata.coordinates.ViewCoordinates;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.IdentifierService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.TaxonomyService;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.constants.Constants;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilderService;
import gov.vha.isaac.ochre.api.memory.HeapUseTicker;
import gov.vha.isaac.ochre.api.progress.ActiveTasksTicker;
import gov.vha.isaac.ochre.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.collections.SequenceSet;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ihtsdo.otf.tcc.api.blueprint.ComponentProperty;
import org.ihtsdo.otf.tcc.api.blueprint.ConceptCB;
import org.ihtsdo.otf.tcc.api.blueprint.DescriptionCAB;
import org.ihtsdo.otf.tcc.api.blueprint.IdDirective;
import org.ihtsdo.otf.tcc.api.blueprint.RefexCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RefexDirective;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.coordinate.EditCoordinate;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.description.DescriptionChronicleBI;
import org.ihtsdo.otf.tcc.api.lang.LanguageCode;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.metadata.binding.SnomedMetadataRf2;
import org.ihtsdo.otf.tcc.api.metadata.binding.TermAux;
import org.ihtsdo.otf.tcc.api.refex.RefexChronicleBI;
import org.ihtsdo.otf.tcc.api.refex.RefexType;
import org.ihtsdo.otf.tcc.api.relationship.RelationshipChronicleBI;
import org.ihtsdo.otf.tcc.api.store.TerminologySnapshotDI;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.ihtsdo.otf.tcc.api.uuid.UuidT3Generator;
import org.ihtsdo.otf.tcc.model.cc.concept.ConceptVersion;
import org.ihtsdo.otf.tcc.model.cc.termstore.PersistentStoreI;
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
		System.setProperty(Constants.DATA_STORE_ROOT_LOCATION_PROPERTY, args[0]);
		//System.setProperty(Constants.CHRONICLE_COLLECTIONS_ROOT_LOCATION_PROPERTY, args[0] + "/object-chronicles");
		//System.setProperty(Constants.SEARCH_ROOT_LOCATION_PROPERTY, args[0] + "/search");
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
			logicService.fullClassification(
					StampCoordinates.getDevelopmentLatest(),
					LogicCoordinates.getStandardElProfile(),
					EditCoordinates.getDefaultUserSolorOverlay());

			LogicalExpressionBuilderService expressionBuilderService = LookupService.getService(LogicalExpressionBuilderService.class);
			LogicalExpressionBuilder defBuilder = expressionBuilderService.getLogicalExpressionBuilder();

			SufficientSet(And(ConceptAssertion(Snomed.BLEEDING_FINDING, defBuilder),
					SomeRole(Snomed.FINDING_SITE, ConceptAssertion(Snomed.ABDOMINAL_WALL_STRUCTURE, defBuilder))));

			LogicalExpression abdominalWallBleedingDef = defBuilder.build();

			System.out.println("Created definition:\n\n " + abdominalWallBleedingDef);

			int newSequence = logicService.getConceptSequenceForExpression((LogicGraph) abdominalWallBleedingDef,
					StampCoordinates.getDevelopmentLatest(),
					LogicCoordinates.getStandardElProfile(),
					EditCoordinates.getDefaultUserSolorOverlay());

			ConceptSequenceSet newConcepts = ConceptSequenceSet.of(newSequence);
			logicService.incrementalClassification(StampCoordinates.getDevelopmentLatest(),
					LogicCoordinates.getStandardElProfile(),
					EditCoordinates.getDefaultUserSolorOverlay(), newConcepts);


			SequenceSet kindOfBleedingSequences = taxonomy.getKindOfSequenceSet(bleedingConcept1.getNid(), ViewCoordinates.getDevelopmentInferredLatest());
			System.out.println("\nHas " + kindOfBleedingSequences.size() + " kinds.");

			if (kindOfBleedingSequences.contains(newSequence)) {
				System.out.println("Kind-of set includes new concept " + newSequence);
			} else {
				System.out.println("Error: kind-of set does not include new concept " + newSequence);
			}


			System.out.println("Test rels from root");
			StringBuilder sb = new StringBuilder();
			ConceptChronicleBI root = LookupService.get().getService(PersistentStoreI.class).getConcept(IsaacMetadataAuxiliaryBinding.ISAAC_ROOT.getNid());
			Collection<? extends RelationshipChronicleBI> incomingRels = root.getRelationshipsIncoming();
			AtomicInteger relCount = new AtomicInteger(1);
			if (incomingRels.isEmpty()) {
				System.out.println(" No incoming rels for: " + root);
			} else {
				System.out.println(" Found " + incomingRels.size() + " incoming rels for: " + root);
				incomingRels.forEach((rel) -> {
					sb.append(relCount.getAndIncrement()).append(": ").append(rel).append("\n");});

			}
			System.out.println(sb.toString());

		} catch (Throwable ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		try {
			// BEGIN test 
			{
				System.out.println("Testing creation and save of nested aspects");
				String testName = "BogusName";
				String refexFSN = testName + " FSN";
				String refexPreferredTerm = testName + " PT";
				String refexDescription = "Description for " + testName;
				UUID testParentUuid = Snomed.ORGANISM.getPrimodialUuid();
				System.out.println("Parent UUID: " + testParentUuid);
				boolean annotationStyle = true;
				ViewCoordinate vc = ViewCoordinates.getMetadataViewCoordinate();
				System.out.println("ViewCoordinate UUID: " + vc.getVcUuid());

				LanguageCode lc = LanguageCode.EN_US;
				UUID isA = Snomed.IS_A.getUuids()[0];
				IdDirective idDir = IdDirective.GENERATE_HASH;
				UUID module = TermAux.ISAAC_MODULE.getPrimodialUuid();
				System.out.println("ISAAC_MODULE UUID: " + module);
				UUID parents[] = new UUID[] { testParentUuid };
				UUID path = null; // TODO get the path set right...

				ConceptCB cab = new ConceptCB(refexFSN, refexPreferredTerm, lc, isA, idDir, module, path, parents);
				cab.setAnnotationRefexExtensionIdentity(annotationStyle);

				DescriptionCAB dCab = new DescriptionCAB(cab.getComponentUuid(), Snomed.DEFINITION_DESCRIPTION_TYPE.getUuids()[0], lc, refexDescription, true,
						IdDirective.GENERATE_HASH);
				dCab.getProperties().put(ComponentProperty.MODULE_ID, module);

				//Mark it as preferred
				RefexCAB rCabPreferred = new RefexCAB(RefexType.CID, dCab.getComponentUuid(), 
						Snomed.US_LANGUAGE_REFEX.getUuids()[0], IdDirective.GENERATE_HASH, RefexDirective.EXCLUDE);
				rCabPreferred.put(ComponentProperty.COMPONENT_EXTENSION_1_ID, SnomedMetadataRf2.PREFERRED_RF2.getUuids()[0]);
				rCabPreferred.getProperties().put(ComponentProperty.MODULE_ID, module);
				dCab.addAnnotationBlueprint(rCabPreferred);

				cab.addDescriptionCAB(dCab);

				//Build this on the lowest level path, otherwise, other code that references this will fail (as it doesn't know about custom paths)
				System.out.println("Constructing ConceptCB...");
				ConceptChronicleBI newCon = Ts.get().getTerminologyBuilder(
						new EditCoordinate(TermAux.USER.getLenient().getConceptNid(), 
								TermAux.ISAAC_MODULE.getLenient().getNid(), 
								TermAux.WB_AUX_PATH.getLenient().getConceptNid()), 
								vc).construct(cab);

				System.out.println("Adding uncommitted...");
				Ts.get().addUncommitted(newCon);

				System.out.println("Committing...");
				Ts.get().commit();

				System.out.println("Constructed concept " + newCon.getVersion(vc).get());
				System.out.println("Constructed concept " + newCon.getVersion(vc).get().toLongString());

				System.out.println("Displaying concept description refexes");
				for (DescriptionChronicleBI desc : newCon.getDescriptions()) {
					System.out.println("Displaying concept description " + desc + " annotations");
					int numAnnotations = 0;
					for (RefexChronicleBI<?> annotation : desc.getAnnotations()) {
						numAnnotations++;
						System.out.println("Desc " + desc + " has annotation " + annotation);
					}
					System.out.println("Desc " + desc + " has " + numAnnotations + " annotations");

					int numRefexes = 0;
					System.out.println("Displaying concept description " + desc + " refexes");
					for (RefexChronicleBI<?> refex : desc.getRefexes()) {
						numRefexes++;
						System.out.println("Desc " + desc + " has refex " + refex);
					}
					System.out.println("Desc " + desc + " has " + numRefexes + " refexes");
				}
			}
			// END test

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
