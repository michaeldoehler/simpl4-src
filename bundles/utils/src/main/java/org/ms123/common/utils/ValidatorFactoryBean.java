/**
 * This file is part of SIMPL4(http://simpl4.org).
 *
 * 	Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
 *
 * SIMPL4 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SIMPL4 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SIMPL4.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ms123.common.utils;
import org.hibernate.validator.*;
import javax.validation.*;
import javax.validation.bootstrap.*;

import java.util.*;

@SuppressWarnings("unchecked")
public final class ValidatorFactoryBean {  

	/**
	 * Custom provider resolver is needed since the default provider resolver 
	 * relies on current thread context loader and doesn't find the default 
	 * META-INF/services/.... configuration file 
	 * 
	 */  
	private static class HibernateValidationProviderResolver implements ValidationProviderResolver {  
		@Override  
		public List getValidationProviders() {  
			List providers = new ArrayList(1);  
			providers.add(new HibernateValidator());  
			return providers;  
		}  
	}  

	private final static ValidatorFactory instance;  

	static {  
		ProviderSpecificBootstrap validationBootStrap = Validation.byProvider(HibernateValidator.class);  
		System.out.println("validationBootStrap:"+validationBootStrap);

		validationBootStrap.providerResolver(new HibernateValidationProviderResolver());  
		instance = validationBootStrap.configure().buildValidatorFactory();  
		System.out.println("instance:"+instance);
	}  

	public final static ValidatorFactory getInstance() {  
		return instance;  
	}  
}  

