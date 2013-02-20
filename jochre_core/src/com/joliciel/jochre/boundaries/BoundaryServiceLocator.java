///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.jochre.boundaries;

import javax.sql.DataSource;

import com.joliciel.jochre.JochreServiceLocator;

public class BoundaryServiceLocator {
	BoundaryServiceImpl boundaryService = null;
	BoundaryDao boundaryDao = null;
	private JochreServiceLocator jochreServiceLocator;
	
	private DataSource dataSource;
	
	public BoundaryServiceLocator(JochreServiceLocator jochreServiceLocator, DataSource dataSource) {
		this.jochreServiceLocator = jochreServiceLocator;
		this.dataSource = dataSource;
	}
	
	public BoundaryService getBoundaryService() {
		if (boundaryService==null) {
			boundaryService = new BoundaryServiceImpl();
			boundaryService.setGraphicsService(this.jochreServiceLocator.getGraphicsServiceLocator().getGraphicsService());
			boundaryService.setBoundaryDao(this.getBoundaryDao());
			boundaryService.setMachineLearningService(this.jochreServiceLocator.getMachineLearningServiceLocator().getMachineLearningService());
		}
		return boundaryService;
	}
	
	BoundaryDao getBoundaryDao() {
		if (this.boundaryDao == null) {
			this.boundaryDao = new BoundaryDaoJdbc();
			this.boundaryDao.setDataSource(dataSource);
		}
		return boundaryDao;
	}

	public JochreServiceLocator getJochreServiceLocator() {
		return jochreServiceLocator;
	}
	
	
}