/**
 * Copyright (C) 2011 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */

package cz.cvut.kbss.jopa.owlapi;

import cz.cvut.kbss.jopa.exceptions.OWLPersistenceException;
import cz.cvut.kbss.jopa.loaders.EntityLoader;
import cz.cvut.kbss.jopa.model.IRI;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.metamodel.EmbeddableType;
import cz.cvut.kbss.jopa.model.metamodel.EntityType;
import cz.cvut.kbss.jopa.model.metamodel.ManagedType;
import cz.cvut.kbss.jopa.model.metamodel.Metamodel;
import cz.cvut.kbss.jopa.owlapi.metamodel.EntityFieldMetamodelProcessor;
import cz.cvut.kbss.ontodriver.OntoDriverProperties;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MetamodelImpl implements Metamodel {

    private static final Logger LOG = Logger.getLogger(Metamodel.class
            .getName());

    private static final String ASPECTJ_CLASS = "org.aspectj.weaver.loadtime.Agent";

    private final Map<Class<?>, EntityType<?>> typeMap = new HashMap<>();

    private final Set<Class<?>> inferredClasses = new HashSet<>();

    private EntityManagerFactoryImpl emf;

    private Set<URI> moduleExtractionSignature;

    private final Set<Class<?>> entities = new HashSet<>();

    MetamodelImpl(final EntityManagerFactoryImpl emf, EntityLoader entityLoader) {
        this.emf = emf;
        build(entityLoader);
    }

    private void build(EntityLoader entityLoader) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Building metamodel ... ");
        }
        checkForWeaver();

        loadEntities(entityLoader);

        entities.forEach((cls) -> processOWLClass(cls));
    }

    /**
     * Check the class path for aspectj weaver, which is vital for using lazy loading.
     */
    private void checkForWeaver() {
        try {
            @SuppressWarnings("unused")
            Class<?> c = MetamodelImpl.class.getClassLoader().loadClass(
                    ASPECTJ_CLASS);
        } catch (ClassNotFoundException e) {
            LOG.severe("AspectJ not found on classpath. Cannot run without AspectJ.");
            throw new OWLPersistenceException(e);
        }
    }

    private void loadEntities(EntityLoader entityLoader) {
        Set<Class<?>> discoveredEntities = entityLoader.discoverEntityClasses(emf.getProperties());
        entities.addAll(discoveredEntities);
    }

    private <X> void processOWLClass(final Class<X> cls) {
        if (typeMap.containsKey(cls)) {
            return;
        }

        if (LOG.isLoggable(Level.CONFIG)) {
            LOG.config("processing OWL class : " + cls);
        }

        final OWLClass c = cls.getAnnotation(OWLClass.class);

        if (c == null) {
            throw new OWLPersistenceException("The class " + cls
                    + " is not an OWLPersistence entity!");
        }

        final EntityTypeImpl<X> c2 = new EntityTypeImpl<>(cls.getSimpleName(), cls, IRI.create(c.iri()));

        typeMap.put(cls, c2);
        final EntityFieldMetamodelProcessor<X> fieldProcessor = new EntityFieldMetamodelProcessor<>(cls, c2, this);

        for (final Field field : cls.getDeclaredFields()) {
            fieldProcessor.processField(field);
        }

        if (c2.getIdentifier() == null) {
            throw new IllegalArgumentException();
        }
    }

    @SuppressWarnings("unchecked")
    public <X> EntityType<X> entity(Class<X> cls) {
        if (!typeMap.containsKey(cls)) {
            // TODO
            processOWLClass(cls);
        }

        return (EntityType<X>) typeMap.get(cls);
    }

    public <X> EmbeddableType<X> embeddable(Class<X> cls) {
        throw new IllegalArgumentException("Embeddable entities not supported.");
    }

    public Set<EmbeddableType<?>> getEmbeddables() {
        return Collections.emptySet();
    }

    public Set<EntityType<?>> getEntities() {
        return new HashSet<>(typeMap.values());
    }

    public Set<ManagedType<?>> getManagedTypes() {
        return new HashSet<>(typeMap.values());
    }

    public <X> ManagedType<X> managedType(Class<X> cls) {
        return entity(cls);
    }

    public Set<Class<?>> getInferredClasses() {
        return Collections.unmodifiableSet(inferredClasses);
    }

    public void addInferredClass(Class<?> cls) {
        inferredClasses.add(cls);
    }

    @Override
    public Set<URI> getModuleExtractionExtraSignature() {
        return Collections.unmodifiableSet(getSignatureInternal());
    }

    @Override
    public void addUriToModuleExtractionSignature(URI uri) {
        if (uri == null) {
            throw new NullPointerException();
        }
        synchronized (this) {
            getSignatureInternal().add(uri);
        }
    }

    private synchronized Set<URI> getSignatureInternal() {
        // This can be lazily loaded since we don'attributeType know if we'll need it
        if (moduleExtractionSignature == null) {
            final String sig = emf.getProperties().get(OntoDriverProperties.MODULE_EXTRACTION_SIGNATURE);
            if (sig == null) {
                this.moduleExtractionSignature = new HashSet<>();
            } else {
                final String[] signature = sig.split(OntoDriverProperties.SIGNATURE_DELIMITER);
                this.moduleExtractionSignature = new HashSet<>(signature.length);
                try {
                    for (String uri : signature) {
                        moduleExtractionSignature.add(new URI(uri));
                    }
                } catch (URISyntaxException e) {
                    throw new OWLPersistenceException("Invalid URI encountered in module extraction signature.", e);
                }
            }
        }
        return moduleExtractionSignature;
    }
}
