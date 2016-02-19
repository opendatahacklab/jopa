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
package cz.cvut.kbss.ontodriver.model;

/**
 * Represents either an OWL axiom or a RDF triple. </p>
 * 
 * 
 * @author ledvima1
 * 
 */
public interface Axiom<T> {

	/**
	 * Gets the subject of this axiom.
	 * 
	 * @return Subject named resource
	 */
	public NamedResource getSubject();

	/**
	 * Gets the assertion of this axiom.
	 * 
	 * @return Assertion named resource
	 */
	public Assertion getAssertion();

	/**
	 * Gets the value of this axiom.
	 * 
	 * @return Value, either a NamedResource, or a literal value
	 */
	public Value<T> getValue();
}