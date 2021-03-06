/* 
 * Copyright(C) Triniforce 
 * All Rights Reserved. 
 * 
 */

package com.triniforce.db.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.InvalidPropertiesFormatException;
import java.util.Set;

import com.triniforce.db.ddl.ActualStateBL;
import com.triniforce.db.ddl.DBTables;
import com.triniforce.db.ddl.TableDef;
import com.triniforce.db.ddl.UpgradeRunner;
import com.triniforce.db.ddl.UpgradeRunner.DbType;
import com.triniforce.server.srvapi.IDatabaseInfo;
import com.triniforce.server.srvapi.ISODbInfo;
import com.triniforce.server.srvapi.ISOQuery.EServerObjectNotFound;
import com.triniforce.utils.Api;
import com.triniforce.utils.ApiAlgs;
import com.triniforce.utils.ApiStack;

public class DBTestCase extends TFTestCase {
    
    public final static String UnicodeSample ="۞∑русскийڧüöäë";
    
    private Connection m_conn= null;

	private ActualStateBL m_as;
    static boolean bCleanDB = true;
    

    protected String getURL(){
        return getTestProperty("url");
    }

    protected Connection getNewConnection(){
    	ApiAlgs.getLog(this).trace("getNewConnection");
        try{
            Connection conn = getDataSource().getConnection();
            conn.setAutoCommit(false);
            return conn;
        }catch(Exception e){
            ApiAlgs.rethrowException(e);
        }
        return null;

    }
    
	protected Connection getConnection() throws Exception{
        if(m_conn == null){
            Connection conn = getNewConnection();

            trace("Testing on \"%s\"", conn.getMetaData().getDatabaseProductName());
            trace("Driver: %s", conn.getMetaData().getDriverName());
            
            conn.setAutoCommit(false);
            
            m_conn = conn;
            m_as = new ActualStateBL(m_conn); 
        }
        
        if(bCleanDB){
            cleanDatabase();

            UpgradeRunner pl = new UpgradeRunner(m_conn, m_as);
            pl.init();
            if(!m_conn.getAutoCommit())
            	m_conn.commit();        
            bCleanDB = false;
        	
            m_as = new ActualStateBL(m_conn);
        }
        
        return m_conn;        
    }  
    
 
    /**
     * Creates table ( if needed ) and removes all rows from it
     * @param tabDef
     * @return database name
     * @throws Exception
     */
    public String createTableIfNeeded(TableDef tabDef) throws Exception{
    	getConnection().setAutoCommit(false);
    	return createTableIfNeeded(tabDef, m_as);
    }
    
    /**
     * Creates table ( if needed ) and removes all rows from it
     * @param tabDef
     * @return database name
     * @throws Exception
     */
    public String createTableIfNeeded(TableDef tabDef, ActualStateBL as) throws Exception{
        
        DBTables ts = new DBTables();
        UpgradeRunner pl = new UpgradeRunner(getConnection(), as);
        ts.add(tabDef);
        ts.setActualState(pl.getActualState());
        pl.run(ts.getCommandList());
        String dbName = pl.getActualState().getDBName(tabDef.getEntityName());
        getConnection().prepareStatement("delete from "+dbName).execute();
        getConnection().commit();
        return dbName;
    }
    
    public static UpgradeRunner.DbType getDbType() throws InvalidPropertiesFormatException, FileNotFoundException, IOException{
        UpgradeRunner.DbType res = UpgradeRunner.DbType.OTHER;
        String url = getTestProperty("url");
        if(url.startsWith("jdbc:derby:"))
            res = UpgradeRunner.DbType.DERBY;
        else if (url.startsWith("jdbc:microsoft:sqlserver:"))
            res = UpgradeRunner.DbType.MSSQL;
        else if (url.startsWith("jdbc:jtds:sqlserver:"))
            res = UpgradeRunner.DbType.MSSQL;        
        else if (url.startsWith("jdbc:mysql:"))
            res = UpgradeRunner.DbType.MYSQL;
        else if (url.startsWith("jdbc:oracle:"))
            res = UpgradeRunner.DbType.ORACLE;
        else if (url.startsWith("jdbc:firebirdsql:"))
            res = UpgradeRunner.DbType.FIREBIRD;
        else if (url.startsWith("jdbc:hsqldb:"))
            res = UpgradeRunner.DbType.HSQL;
        else if (url.startsWith("jdbc:h2:"))
            res = UpgradeRunner.DbType.H2;        
        else if (url.startsWith("jdbc:sqlserver"))
            res = UpgradeRunner.DbType.MSSQL;        
        

        return res;
    }
        
    @Override
    protected void setUp() throws Exception {
        Api api = new Api();
        api.setIntfImplementor(IDatabaseInfo.class, getDbInfo());
        api.setIntfImplementor(ISODbInfo.class, new ISODbInfo() {
			
			public String getTableDbName(String entityName)
					throws EServerObjectNotFound {
				return m_as.getDBName(entityName);
			}
			
			public String getTableAppName(String dbName) throws EServerObjectNotFound {
				try {
					return m_as.getAppName(dbName);
				} catch (SQLException e) {
					ApiAlgs.rethrowException(e);
					return null;
				}
			}
			
			public Set<String> getDbTableNames() {
				return m_as.getDbTableNames();
			}

			public Set<String> getCompletedDataPreparationProcedures() {
				fail("unimplemented");
				return null;
			}

			public Set<String> getCompletedUpgradeProcedures() {
				fail("unimplemented");
				return null;
			}
//
//			public boolean isNewDb() {
//				return false;
//			}
		});
        ApiStack.pushApi(api);
        
//        Connection conn = getConnection();
//        conn.setAutoCommit(false);
        super.setUp();
    }
    
    public static IDatabaseInfo getDbInfo() throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
        final DbType dbType = getDbType();
        final String quoteString = DbType.MYSQL.equals(dbType) ? "`" : "\"";
        return new IDatabaseInfo(){

			public DbType getDbType() {
				return dbType;
			}

			public String getIdentifierQuoteString() {
				return quoteString;
			}
        	
        };
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        ApiStack.popApi();
        clearConnection();
    }
    
    private void cleanDatabase() throws Exception{
    	if(!getDbType().equals(DbType.FIREBIRD))
    		CleanDatabase.run(this, ApiAlgs.getLog(this));
    	else{
//    		CleanDatabase.run(this, ApiAlgs.getLog(this), false);
    	}
   }
    
    public void clearConnection() throws Exception{
        if( null != m_conn){
        	if(!m_conn.getAutoCommit())
        		m_conn.rollback();
            m_conn.close();
            m_conn = null;
        }
    }    
    
    public void test() throws Exception{
    }

	public Connection reopenConnection() throws Exception {
		clearConnection();
		DBTestCase.closeDataSource();
		
        Connection conn = getNewConnection();
        conn.setAutoCommit(false);
        
        m_conn = conn;
//        m_as = new ActualStateBL(m_conn); 

		return m_conn;
	}


	public ActualStateBL getAS(){
		return m_as;
	}
    
}
