/**
 * Copyright (C) 2016 Czech Technical University in Prague
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
package cz.cvut.kbss.jopa.environment;

import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.jopa.model.PersistenceProvider;
import cz.cvut.kbss.jopa.model.ProviderUtil;

import java.util.Map;

public class TestPersistenceProvider implements PersistenceProvider {

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map<String, String> map) {
        return null;
    }

    @Override
    public ProviderUtil getProviderUtil() {
        return null;
    }
}
