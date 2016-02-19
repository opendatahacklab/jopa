/**
 * Copyright (C) 2011 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jopa.query.sparql;

import cz.cvut.kbss.jopa.model.QueryImpl;
import cz.cvut.kbss.jopa.model.TypedQueryImpl;
import cz.cvut.kbss.jopa.model.query.Query;
import cz.cvut.kbss.jopa.model.query.TypedQuery;
import cz.cvut.kbss.jopa.query.QueryParser;
import cz.cvut.kbss.jopa.sessions.ConnectionWrapper;
import cz.cvut.kbss.jopa.sessions.QueryFactory;
import cz.cvut.kbss.jopa.sessions.UnitOfWorkImpl;
import cz.cvut.kbss.jopa.utils.ErrorUtils;

import java.util.Objects;

public class SparqlQueryFactory implements QueryFactory {

    private final UnitOfWorkImpl uow;
    private final ConnectionWrapper connection;

    private final QueryParser queryParser;

    public SparqlQueryFactory(UnitOfWorkImpl uow, ConnectionWrapper connection) {
        assert uow != null;
        assert connection != null;
        this.uow = uow;
        this.connection = connection;
        this.queryParser = new SparqlQueryParser();
    }

    @Override
    public Query createNativeQuery(String sparql) {
        Objects.requireNonNull(sparql, ErrorUtils.constructNPXMessage("sparql"));

        final QueryImpl q = new QueryImpl(queryParser.parseQuery(sparql), connection);
        q.setUseBackupOntology(uow.useBackupOntologyForQueryProcessing());
        return q;
    }

    @Override
    public <T> TypedQuery<T> createNativeQuery(String sparql, Class<T> resultClass) {
        Objects.requireNonNull(sparql, ErrorUtils.constructNPXMessage("sparql"));
        Objects.requireNonNull(resultClass, ErrorUtils.constructNPXMessage("resultClass"));

        final TypedQueryImpl<T> tq = new TypedQueryImpl<>(queryParser.parseQuery(sparql), resultClass, connection, uow);
        tq.setUnitOfWork(uow);
        tq.setUseBackupOntology(uow.useBackupOntologyForQueryProcessing());
        return tq;
    }

    @Override
    public Query createQuery(String query) {
        Objects.requireNonNull(query, ErrorUtils.constructNPXMessage("query"));

        // We do not support any more abstract syntax, yet
        return createNativeQuery(query);
    }

    @Override
    public <T> TypedQuery<T> createQuery(String query, Class<T> resultClass) {
        Objects.requireNonNull(query, ErrorUtils.constructNPXMessage("query"));
        Objects.requireNonNull(resultClass, ErrorUtils.constructNPXMessage("resultClass"));

        // We do not support any more abstract syntax, yet
        return createNativeQuery(query, resultClass);
    }
}