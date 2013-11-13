package cz.cvut.kbss.jopa.accessors;

import java.util.Map;

import cz.cvut.kbss.jopa.model.metamodel.Metamodel;
import cz.cvut.kbss.jopa.sessions.Session;

public interface OntologyAccessorFactory {

	/**
	 * Create an ontology accessor instance based on the specified data holder,
	 * which contains data about the ontology storage.
	 * 
	 * @param dataHolder
	 *            Data holder for ontology storage information
	 * @param session
	 *            Parent session.
	 * @return Initialized accessor instance
	 */
	public TransactionOntologyAccessor createTransactionalAccessor(
			OntologyDataHolder dataHolder, Session session);

	/**
	 * Create an ontology accessor instance based on the specified properties
	 * (especially important are information about storage type) and initialize
	 * it with metamodel.
	 * 
	 * @param properties
	 *            Map of properties for the accessor
	 * @param metamodel
	 *            Metamodel of entities
	 * @param session
	 *            Parent session
	 * @return Initialized accessor instance
	 */
	public OntologyAccessor createCentralAccessor(
			Map<String, String> properties, Metamodel metamodel, Session session);

}