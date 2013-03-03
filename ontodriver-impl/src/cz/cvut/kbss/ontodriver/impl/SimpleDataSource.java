package cz.cvut.kbss.ontodriver.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cz.cvut.kbss.ontodriver.Connection;
import cz.cvut.kbss.ontodriver.DataSource;
import cz.cvut.kbss.ontodriver.OntoDriver;
import cz.cvut.kbss.ontodriver.OntoDriverProperties;
import cz.cvut.kbss.ontodriver.OntologyStorageProperties;
import cz.cvut.kbss.ontodriver.PersistenceProvider;
import cz.cvut.kbss.ontodriver.StorageManager;
import cz.cvut.kbss.ontodriver.exceptions.OntoDriverException;

/**
 * Simple data source implementation without any pooling. </p>
 * 
 * For each request a new connection is created.
 * 
 * @author kidney
 * 
 */
public class SimpleDataSource implements DataSource {

	private final OntoDriver driver;
	private final Map<String, String> properties;

	public SimpleDataSource(List<OntologyStorageProperties> storageProperties) {
		if (storageProperties == null || storageProperties.isEmpty()) {
			throw new IllegalArgumentException(
					"StorageProperties cannot be neither null nor empty.");
		}
		this.properties = Collections.emptyMap();
		this.driver = new OntoDriverImpl(storageProperties, properties);
	}

	public SimpleDataSource(List<OntologyStorageProperties> storageProperties,
			Map<String, String> properties) {
		super();
		if (storageProperties == null || storageProperties.isEmpty()) {
			throw new IllegalArgumentException(
					"StorageProperties cannot be neither null nor empty.");
		}
		if (properties == null) {
			properties = Collections.emptyMap();
		}
		this.properties = properties;
		this.driver = new OntoDriverImpl(storageProperties, properties);
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	public Connection getConnection() throws OntoDriverException {
		throw new UnsupportedOperationException();
	}

	public Connection getConnection(PersistenceProvider persistenceProvider)
			throws OntoDriverException {
		return createConnection(persistenceProvider);
	}

	private Connection createConnection(PersistenceProvider persistenceProvider)
			throws OntoDriverException {
		final StorageManager sm = driver
				.acquireStorageManager(persistenceProvider);
		final Connection conn = new ConnectionImpl(sm);
		final String strAutoCommit = properties
				.get(OntoDriverProperties.CONNECTION_AUTO_COMMIT);
		boolean autoCommit = false;
		if (strAutoCommit != null) {
			autoCommit = Boolean.parseBoolean(strAutoCommit);
		}
		conn.setAutoCommit(autoCommit);
		return conn;
	}
}
