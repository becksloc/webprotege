package edu.stanford.bmir.protege.web.server.mansyntax;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import edu.stanford.bmir.protege.web.server.inject.project.RootOntology;
import edu.stanford.bmir.protege.web.shared.frame.HasFreshEntities;
import edu.stanford.bmir.protege.web.shared.frame.ManchesterSyntaxFrameParseError;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntax;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxFramesParser;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxTokenizer;
import org.coode.owlapi.manchesterowlsyntax.OntologyAxiomPair;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.OWLOntologyChecker;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics Research Group, Date: 20/03/2014
 */
public class ManchesterSyntaxFrameParser {

    private final OWLOntology rootOntology;

    private final OWLDataFactory dataFactory;

    private final BidirectionalShortFormProvider shortFormProvider;

    private final OWLOntologyChecker ontologyChecker;

    @Inject
    public ManchesterSyntaxFrameParser(@RootOntology OWLOntology rootOntology,
                                       BidirectionalShortFormProvider shortFormProvider,
                                       OWLOntologyChecker ontologyChecker,
                                       OWLDataFactory dataFactory) {
        this.shortFormProvider = shortFormProvider;
        this.ontologyChecker = ontologyChecker;
        this.dataFactory = dataFactory;
        this.rootOntology = rootOntology;
    }

    public Set<OntologyAxiomPair> parse(String syntax, HasFreshEntities hasFreshEntities) throws ParserException {
        OWLEntityChecker entityChecker = new WebProtegeOWLEntityChecker(
                shortFormProvider,
                hasFreshEntities
        );
        ManchesterOWLSyntaxFramesParser parser = new ManchesterOWLSyntaxFramesParser(dataFactory, entityChecker);
        parser.setOWLOntologyChecker(ontologyChecker);
        parser.setDefaultOntology(rootOntology);
        return parser.parse(syntax);
    }

    public static ManchesterSyntaxFrameParseError getParseError(ParserException e) {
        List<EntityType<?>> expectedEntityTypes = getExpectedEntityTypes(e);
        String message = e.getMessage().replace(ManchesterOWLSyntaxTokenizer.EOF, "end of description");
        return new ManchesterSyntaxFrameParseError(message,
                e.getColumnNumber(),
                e.getLineNumber(),
                e.getCurrentToken(),
                expectedEntityTypes);
    }

    public static List<EntityType<?>> getExpectedEntityTypes(ParserException e) {
        String currentToken = e.getCurrentToken();
        if (isManchesterSyntaxKeyword(currentToken)) {
            return Collections.emptyList();
        }
        if(e.getCurrentToken().equals(ManchesterOWLSyntaxTokenizer.EOF)) {
            return Collections.emptyList();
        }
        List<EntityType<?>> expectedEntityTypes = Lists.newArrayList();
        if(e.isClassNameExpected()) {
            expectedEntityTypes.add(EntityType.CLASS);
        }
        if(e.isIndividualNameExpected()) {
            expectedEntityTypes.add(EntityType.NAMED_INDIVIDUAL);
        }
        if(e.isObjectPropertyNameExpected()) {
            expectedEntityTypes.add(EntityType.OBJECT_PROPERTY);
        }
        if(e.isDataPropertyNameExpected()) {
            expectedEntityTypes.add(EntityType.DATA_PROPERTY);
        }
        if(e.isAnnotationPropertyNameExpected()) {
            expectedEntityTypes.add(EntityType.ANNOTATION_PROPERTY);
        }
        if(e.isDatatypeNameExpected()) {
            expectedEntityTypes.add(EntityType.DATATYPE);
        }
        return expectedEntityTypes;
    }

    private static boolean isManchesterSyntaxKeyword(String currentToken) {
        String strippedToken;
        if(currentToken.endsWith(":")) {
            strippedToken = currentToken.substring(0, currentToken.length() - 1);
        }
        else {
            strippedToken = currentToken;
        }
        for(ManchesterOWLSyntax syntax : ManchesterOWLSyntax.values()) {
            if(strippedToken.equals(syntax.keyword())) {
                return true;
            }
        }
        return false;
    }


}
