package cz.cvut.kbss.jopa.test;

import cz.cvut.kbss.jopa.model.annotations.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Set;

@OWLClass(iri = "http://krizik.felk.cvut.cz/ontologies/jopa/entities#OWLClassJ")
public class OWLClassJ {

	private static final String CLS_A_FIELD = "owlClassA";

	@Id
	private URI uri;

	@OWLObjectProperty(iri = "http://krizik.felk.cvut.cz/ontologies/jopa/attributes#hasA", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	// @ParticipationConstraints({
	// @ParticipationConstraint(owlObjectIRI="http://new.owl#OWLClassA", min=1,
	// max=1)
	// })
	private Set<OWLClassA> owlClassA;

	/**
	 * @param uri
	 *            the uri to set
	 */
	public void setUri(URI uri) {
		this.uri = uri;
	}

	/**
	 * @return the uri
	 */
	public URI getUri() {
		return uri;
	}

	public void setOwlClassA(Set<OWLClassA> owlClassA) {
		this.owlClassA = owlClassA;
	}

	public Set<OWLClassA> getOwlClassA() {
		return owlClassA;
	}

	public static String getClassIri() {
		return OWLClassJ.class.getAnnotation(OWLClass.class).iri();
	}
	
	public static Field getOwlClassAField() throws NoSuchFieldException, SecurityException {
		return OWLClassJ.class.getDeclaredField(CLS_A_FIELD);
	}
}
