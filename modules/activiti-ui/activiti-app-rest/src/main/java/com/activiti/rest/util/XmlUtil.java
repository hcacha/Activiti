/**
 * Activiti app component part of the Activiti project
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.activiti.rest.util;

import javax.xml.stream.XMLInputFactory;

/**
 * @author Joram Barrez
 */
public class XmlUtil {

	/**
	 * 'safe' is here reflecting:
	 * http://www.jorambarrez.be/blog/2013/02/19/uploading
	 * -a-funny-xml-can-bring-down-your-server/ and
	 * http://activiti.org/userguide/index.html#advanced.safe.bpmn.xml
	 */
	public static XMLInputFactory createSafeXmlInputFactory() {
		XMLInputFactory xif = XMLInputFactory.newInstance();
		if (xif.isPropertySupported(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES)) {
			xif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,
			        false);
		}

		if (xif.isPropertySupported(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)) {
			xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
			        false);
		}

		if (xif.isPropertySupported(XMLInputFactory.SUPPORT_DTD)) {
			xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		}
		return xif;
	}

}
