/**
 * Copyright (C) 2020 Czech Technical University in Prague
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
package cz.cvut.kbss.ontodriver.owlapi.query;

import cz.cvut.kbss.ontodriver.exception.OntoDriverException;
import cz.cvut.kbss.ontodriver.owlapi.connector.Connector;
import cz.cvut.kbss.ontodriver.ResultSet;
import cz.cvut.kbss.ontodriver.Statement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class LiveOntologyStatementExecutorTest {

    private static final String QUERY = "SELECT ?x ?y ?z WHERE { ?x ?y ?z . }";
    private static final String UPDATE = "INSERT DATA { a rdf:type b . }";

    @Mock
    private Connector connectorMock;
    @Mock
    private Statement statementMock;

    private LiveOntologyStatementExecutor executor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.executor = new LiveOntologyStatementExecutor(connectorMock);
    }

    @Test
    public void executeQueryPassesFunctionToConnectorForExecutionInReadOnly() throws Exception {
        final SelectResultSet resultSet = mock(SelectResultSet.class);
        when(connectorMock.executeRead(any(Function.class))).thenReturn(resultSet);
        final ResultSet res = executor.executeQuery(QUERY, statementMock);
        assertNotNull(res);
        assertEquals(resultSet, res);
        verify(connectorMock).executeRead(any(Function.class));
    }

    @Test(expected = OntoDriverException.class)
    public void executeQueryReturningNullThrowsDriverException() throws Exception {
        executor.executeQuery(QUERY, statementMock);
    }

    @Test
    public void executeUpdatePassesConsumerFunctionToConnectorForExecutionInWriteMode() throws Exception {
        executor.executeUpdate(UPDATE);
        verify(connectorMock).executeWrite(any(Consumer.class));
    }
}