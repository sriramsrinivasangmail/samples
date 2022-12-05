package com.ibm.datatools.dsweb.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.BatchUpdateException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SQLUtil
{
    /**
     * YOU MUST call close() right below to close the result set and the
     * statement !
     * 
     * @param conn
     * @param sql
     * @return
     * @throws SQLException
     */
    public static ResultSet executeQuery(Connection conn, String sql) throws SQLException
    {
        Statement stmt = conn.createStatement();
        boolean resultsExist = stmt.execute(sql);
        if (resultsExist)
        {
            ResultSet r = stmt.getResultSet();
            return r;
        }
        
        return null;
    }
    
    public static void close(ResultSet r) throws SQLException
    {
        if (null != r)
        {
            Statement stmt = r.getStatement();
            r.close();
            if (null != stmt)
            {
                stmt.close();
            }
        }
    }

    public static void rollbackConnection(Connection conn)
    {
        if(null != conn)
        {
            try
            {
                conn.rollback();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void commitConnection(Connection conn)
    {
        if(null != conn)
        {
            try
            {
                conn.commit();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }


    public static void closeConnection(Connection conn)
    {
        if(null != conn)
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static final String TAB = "\t";
    
    public static final String DOT = ".";
    
    public static final String SPACE = " ";
    
    public static final String CR = " \n";
    
    public static final String SQL_CREATE_TABLE = "CREATE TABLE";
    
    public static final String SQL_DROP_TABLE = "DROP TABLE";
    
    public static final String SQL_INSERT_INTO = "INSERT INTO";
    
    public static final String SQL_UPDATE = "UPDATE";
    
    public static final String SQL_SELECT = "SELECT";
    
    public static final String SQL_DELETE = "DELETE";
    
    public static final String SQL_FROM = "FROM";
    
    public static final String SQL_VALUES = "VALUES";
    
    public static final String SQL_AS = "AS";
    
    public static final String SQL_SET = "SET";
    
    public static final String SQL_WHERE = "WHERE";
    
    public static final String SQL_ORDER_BY = "ORDER BY";
    
    public static final String SQL_AND = "AND";
    
    public static final String SQL_COLUMN_SEPARATOR = ",";
    
    public static final String SQL_PARAM_INDICATOR = "?";
    
    public static final String SQL_DEFAULT_DATA_TYPE = "VARCHAR(255)";
    
    public static void outputResultSet(ResultSet r, PrintStream out) throws SQLException
    {
        
        ResultSetMetaData meta = r.getMetaData();
        int colCount = meta.getColumnCount();
        
        String header = "";
        for (int column = 1; column <= colCount; column++)
        {
            header = header + meta.getCatalogName(column) + DOT + meta.getSchemaName(column) + DOT + meta.getTableName(column)
                                            + DOT + meta.getColumnLabel(column) + TAB + meta.getColumnTypeName(column) + SPACE
                                            + meta.getPrecision(column) + SPACE + meta.getScale(column) + CR;
        }
        out.println(header);
        while (r.next())
        {
            for (int k = 1; k <= colCount; k++)
            {
                out.print(TAB + TAB + r.getObject(k));
            }
            out.println();
        }
    }
    
    public static int[] getColumnTypes(ResultSet rs)
    {
        try
        {
            ResultSetMetaData metaData = rs.getMetaData();
            int colNum = metaData.getColumnCount();
            int[] colTypes = new int[colNum];
            for (int i = 0; i < colNum; ++i)
            {
                colTypes[i] = metaData.getColumnType(i + 1);
            }
            return colTypes;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
    
    public static String[] getColumnNames(ResultSet rs)
    {
        try
        {
            ResultSetMetaData metaData = rs.getMetaData();
            int colNum = metaData.getColumnCount();
            String[] colNames = new String[colNum];
            for (int i = 0; i < colNum; ++i)
            {
                colNames[i] = metaData.getColumnLabel(i + 1);
            }
            return colNames;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * see next method..
     */
    public static Throwable handleJDBCExceptions(Throwable e, int stmtExecuted, int rowsUpdated, List errMsgs)
    {
        e = handleJDBCExceptions(e, stmtExecuted, rowsUpdated, errMsgs, null);
        return e;
    }
    
    /**
     * see next method..
     */
    public static Throwable handleJDBCExceptions(Throwable e, int stmtExecuted, int rowsUpdated, List errMsgs, List sqlStates)
    {
        e = handleJDBCExceptions(e, stmtExecuted, rowsUpdated, errMsgs, null, null);
        return e;
    }
    
    /**
     * TODO: i18n (so far this has been used for internal purposes.. ..) general
     * purpose routine to capture error messages from a JDBC-caused exception..
     * note.. its not always SQL Exceptions that are thrown..
     * 
     * @param e
     *            the exception you got from any of your JDBC ops..
     * @param stmtExecuted
     *            - if you have been running a batch op, then, provide which
     *            stmt in the batch failed, else 0.
     * @param rowsUpdated
     *            - if you have been keeping track of the number of rows
     *            updated, else 0
     * @param errMsgs
     *            - provide a list to fill in the error messages with.. in some
     *            cases we need to scroll through the trail of 'next exceptions'
     *            to find the right one..
     * @param sqlStates
     *            - provide a list of sqlStates that you consider 'acceptable'.
     * @param sqlExceptionInfo
     *            - if not null, returns a list of Strings with SQLSTATE &
     *            ERRORCODE info..
     */
    
    public static Throwable handleJDBCExceptions(Throwable e, int stmtExecuted, int rowsUpdated, List errMsgs, List sqlStates,
                                    List sqlExceptionInfo)
    {
        int stmtPos = 0;
        int updateCount = 0;
        Throwable realException = e;
        if (e instanceof ClassNotFoundException)
        {
            errMsgs.add("ClassNotFoundException. error message = " + e.getLocalizedMessage());
        }
        else if (e instanceof SQLException)
        {
            SQLException thisEx = (SQLException) e;
            do
            {
                realException = thisEx;
                boolean skip = false;
                String sqlState = thisEx.getSQLState();
                int errorCode = thisEx.getErrorCode();
                if (null != sqlExceptionInfo)
                {
                    sqlExceptionInfo.add("SQLSTATE=" + sqlState + "&ERRORCODE=" + errorCode);
                }
                if (sqlStates != null)
                {
                    for (int i = 0; i < sqlStates.size(); ++i)
                    {
                        if (sqlState.trim().equalsIgnoreCase(((String) sqlStates.get(i)).trim()))
                        {
                            skip = true;
                            break;
                        }
                    }
                }
                if (!skip)
                {
                    StringBuffer strBuf = new StringBuffer();
                    
                    if (thisEx instanceof BatchUpdateException)
                    {
                        int[] updateCounts = ((BatchUpdateException) thisEx).getUpdateCounts();
                        for (int i = 0; i < updateCounts.length; ++i)
                        {
                            if (updateCounts[i] == Statement.EXECUTE_FAILED)
                            {
                                stmtPos = i + 1;
                                break;
                            }
                            // else if(updateCounts[i] !=
                            // Statement.SUCCESS_NO_INFO)
                            // {
                            // updateCount += updateCounts[i];
                            // }
                        }
                        String th = null;
                        switch (stmtPos + stmtExecuted)
                        {
                            case 1:
                                th = new String("st");
                                break;
                            case 2:
                                th = new String("nd");
                                break;
                            case 3:
                                th = new String("rd");
                                break;
                            default:
                                th = new String("th");
                                break;
                        }
                        strBuf.append("Batch operation exception. Operation failed on the " + (stmtPos + stmtExecuted) + th
                                                        + " statement. " + (rowsUpdated) + " rows were inserted/updated" + ". ");
                    }
                    else
                    {
                        strBuf.append("SQL Exception. ");
                    }
                    
                    strBuf.append("SQL state = " + sqlState + "; error code = " + thisEx.getErrorCode() + "; error Message = "
                                                    + thisEx.getLocalizedMessage());
                    
                    errMsgs.add(strBuf.toString());
                }
                thisEx = thisEx.getNextException();
            }
            while (thisEx != null);
            
        }
        else if (e instanceof IOException)
        {
            errMsgs.add("IOException. error message = " + e.getLocalizedMessage());
        }
        else
        {
            errMsgs.add("Exception. error message = " + e.getLocalizedMessage());
        }
        return realException;
    }
    
    // TODO: MAP EMF/JAVA data types to SQL data types (and scale, precision)
    public static void setParamObject(PreparedStatement stmt, int pIdx, int jdbcType, int scale, Object value)
                                    throws SQLException
    {
        if (value == null)
        {
            stmt.setNull(pIdx, jdbcType);
        }
        else
        {
            // conversion based on Java types
            if (value instanceof Boolean)
            {
                boolean bVal = ((Boolean) value).booleanValue();
                switch (jdbcType)
                {
                    case Types.INTEGER:
                    case Types.BIGINT:
                    {
                        if (bVal)
                        {
                            value = new Integer("1");
                        }
                        else
                        {
                            value = new Integer("0");
                        }
                        break;
                    }
                    default:
                    {
                        if (bVal)
                        {
                            value = "1";
                        }
                        else
                        {
                            value = "0";
                        }
                        break;
                    }
                }
            }
            
            // conversion based on the dest SQL types
            switch (jdbcType)
            {
                case Types.BINARY:
                case Types.VARBINARY:
                case Types.LONGVARBINARY:
                    if (value instanceof String)
                    {
                        byte[] bytes = null;
                        try
                        {
                            bytes = convertStringToByteArray((String) value);
                        }
                        catch (NumberFormatException e)
                        {
                            bytes = ((String) value).getBytes();
                        }
                        
                        stmt.setBytes(pIdx, bytes);
                    }
                    else if (value instanceof byte[])
                    {
                        stmt.setBytes(pIdx, ((byte[]) value));
                    }
                    else
                    {
                        stmt.setObject(pIdx, value, jdbcType, scale);
                    }
                    break;
                case Types.DATE:
                    if (value instanceof Long)
                    {
                        long longVal = 0;
                        try
                        {
                            longVal = ((Long) value).longValue();
                        }
                        catch (RuntimeException e)
                        {
                        }
                        Date dtObj = new Date(longVal);
                        value = dtObj;
                    }
                    stmt.setObject(pIdx, value, jdbcType, scale);
                    break;
                case Types.TIMESTAMP:
                    if (value instanceof Long)
                    {
                        long longVal = 0;
                        try
                        {
                            longVal = ((Long) value).longValue();
                        }
                        catch (RuntimeException e)
                        {
                        }
                        Timestamp tsObj = new Timestamp(longVal);
                        value = tsObj;
                    }
                    stmt.setObject(pIdx, value, jdbcType, scale);
                    break;
                default:
                    stmt.setObject(pIdx, value, jdbcType, scale);
            }
        }
    }
    
    public static Object castToJavaType(Object sqlValue, Class castToJavaClassType)
    {
        // System.out.println("CastToJavaType: class = " +
        // castToJavaClassType.getName() + ", value =" + sqlValue);
        
        Object castedJavaInstance = sqlValue;
        if (null != sqlValue)
        {
            if (String.class == castToJavaClassType)
            {
                castedJavaInstance = sqlValue.toString();
            }
            else if ((Boolean.class == castToJavaClassType) || (boolean.class == castToJavaClassType))
            {
                castedJavaInstance = Boolean.TRUE;
                String strVal = sqlValue.toString();
                if ("0".equals(strVal))
                {
                    castedJavaInstance = Boolean.FALSE;
                }
                else if ("N".equals(strVal))
                {
                    castedJavaInstance = Boolean.FALSE;
                }
            }
            else if (sqlValue instanceof Date)
            {
                if ((Long.class == castToJavaClassType) || (long.class == castToJavaClassType))
                {
                    castedJavaInstance = new Long(((Date) sqlValue).getTime());
                }
            }
            else if (sqlValue instanceof Timestamp)
            {
                if ((Long.class == castToJavaClassType) || (long.class == castToJavaClassType))
                {
                    castedJavaInstance = new Long(((Timestamp) sqlValue).getTime());
                }
            }
            else if (sqlValue instanceof BigDecimal)
            {
                // if (BigDecimal.class == castToJavaClassType) {
                
                // System.out.println("Enter BigDecimal");
                castedJavaInstance = new Long(((BigDecimal) sqlValue).longValue());
            }
        }
        return castedJavaInstance;
    }
    
    public final static String SQL_ID_DELIM = "\"";
    
    /**
     * Return a "delimited identifier" if there are anything other than
     * uppercase letter(s) followed by uppercase letter(s), digit(s) or
     * underscore character(s)
     * 
     * @param id
     * @return An identifier that used in SQL statement
     */
    public static String getSQLIdentifier(String id)
    {
        // IDS Support
        // if(id != null && !id.matches("[A-Z]+[_A-Z0-9]*"))
        if (id != null)
            return new String(SQL_ID_DELIM + id + SQL_ID_DELIM);
        else
            return id;
    }
    
    public static String getSQLIdentifier(String schemaName, String tableName)
    {
        StringBuffer strBuf = new StringBuffer();
        if (schemaName != null && schemaName.length() > 0)
        {
            // not process schema name
            strBuf.append(getSQLIdentifier(schemaName) + ".");
        }
        if (tableName != null && tableName.length() > 0)
        {
            strBuf.append(getSQLIdentifier(tableName));
        }
        return strBuf.toString();
    }
    
    public static String getSQLIdentifier(String schemaName, String tableName, String columnName)
    {
        StringBuffer strBuf = new StringBuffer();
        if (schemaName != null && schemaName.length() > 0)
        {
            // not process schema name
            strBuf.append(getSQLIdentifier(schemaName) + ".");
            if (tableName != null && tableName.length() > 0)
            {
                strBuf.append(getSQLIdentifier(tableName));
            }
            strBuf.append(".");
            if (columnName != null && columnName.length() > 0)
            {
                strBuf.append(getSQLIdentifier(columnName));
            }
            
        }
        else
        {
            if (tableName != null && tableName.length() > 0)
            {
                strBuf.append(getSQLIdentifier(tableName) + ".");
            }
            if (columnName != null && columnName.length() > 0)
            {
                strBuf.append(getSQLIdentifier(columnName));
            }
        }
        return strBuf.toString();
    }
    
    public static final String ROW_XML_TAG = "row";
    
    public static final String COLUMN_PROPERTY_XML_TAG = "property";
    
    /**
     * produce a simple XML string that has all of its column values in column
     * tags (name, value properties) and each row in a row tag
     * 
     * @param r
     * @param rowTag
     * @return
     * @throws SQLException
     */
    public static StringBuffer resultSet2XML(ResultSet r, String rowTag, String columnTag) throws SQLException
    {
        StringBuffer buf = new StringBuffer();
        
        ResultSetMetaData meta = r.getMetaData();
        int colCount = meta.getColumnCount();
        
        String header = "";
        for (int column = 1; column <= colCount; column++)
        {
            header = header + meta.getCatalogName(column) + DOT + meta.getSchemaName(column) + DOT
                                            + meta.getTableName(column) + DOT + meta.getColumnLabel(column) + TAB
                                            + meta.getColumnTypeName(column) + SPACE + meta.getPrecision(column)
                                            + SPACE + meta.getScale(column) + CR;
        }
        
        while (r.next())
        {
            buf.append("<" + rowTag + ">\n");
            for (int k = 1; k <= colCount; k++)
            {
                String currColName = meta.getColumnLabel(k);
                Object currColValObj = r.getObject(k);
                if (null != currColValObj)
                {
                    buf.append(TAB + "<" + columnTag + " name=\"" + currColName + "\" value=\"" + currColValObj + "\"/>"
                                                    + CR);
                }
            }
            buf.append("</" + rowTag + ">" + CR);
        }
        return buf;
    }
    
    /**
     * produce a simple XML string that has all of its column values in column
     * tags (name, value properties) and each row in a row tag
     * 
     * @param r
     * @param rowTag
     * @return
     * @throws SQLException
     */
    public static StringBuffer resultSet2XMLWithMetadata(ResultSet r) throws SQLException
    {
        StringBuffer buf = new StringBuffer();
        
        ResultSetMetaData meta = r.getMetaData();
        int colCount = meta.getColumnCount();
        
        buf.append("<metadata>");
        buf.append("<rowid>RowData</rowid>");
        buf.append("<columns>");
        
        for (int column = 1; column <= colCount; column++)
        {
            buf.append("<column num='" + (column - 1) + "' sortable='true' filterable='true' visible='true' >");
            String name = meta.getColumnLabel(column);
            buf.append("<colid>" + name + "</colid>");
            buf.append("<displayname>" + name + "</displayname>");
            
            buf.append("<type>String</type>");
            buf.append("<helptext></helptext>");
            buf.append("<additonalData>");
            buf.append("</additonalData>");
            
            buf.append("</column>");
        }
        
        buf.append("</columns>");
        buf.append("</metadata>");
        buf.append("<rowgroupdata />");
        buf.append("<sortdata>");
        buf.append("</sortdata>");
        buf.append("<filterdata />");
        
        buf.append("<data>");
        
        while (r.next())
        {
            buf.append("<RowData>");
            
            for (int k = 1; k <= colCount; k++)
            {
                String currColName = meta.getColumnLabel(k);
                Object currColValObj = r.getObject(k);
                buf.append("<" + currColName + ">");
                
                if (null != currColValObj)
                {
                    buf.append("" + currColValObj);
                }
                buf.append("</" + currColName + ">");
            }
            buf.append("</RowData>");
        }
        
        buf.append("</data>");
        buf.append("<additonalData>");
        buf.append("</additonalData>");
        
        return buf;
    }
    
    /**
     * return a map where the key is the value of the column represented by
     * keyColIdx. The value of this map is itself a map of the column name,
     * value pairs colIdx starts with 1 (one - not zero)
     * 
     * @param r
     * @param keyColIdx
     * @return
     * @throws SQLException
     */
    public static Map<String, Map<String, String>> rsToMap(ResultSet r, int keyColIdx) throws SQLException
    {
        Map<String, Map<String, String>> allRows = new LinkedHashMap<String, Map<String, String>>();
        ResultSetMetaData meta = r.getMetaData();
        int colCount = meta.getColumnCount();
        
        while (r.next())
        {
            Map<String, String> currRowProps = new LinkedHashMap<String, String>();
            String rowKeyValue = null;
            for (int k = 1; k <= colCount; k++)
            {
                String propName = meta.getColumnLabel(k);
                Object valObj = r.getObject(k);
                if (null != valObj)
                {
                    String propValue = "" + valObj;
                    currRowProps.put(propName, propValue);
                    if (k == keyColIdx)
                    {
                        rowKeyValue = propValue;
                    }
                }
            }
            if (null != rowKeyValue)
            {
                allRows.put(rowKeyValue, currRowProps);
            }
        }
        
        return allRows;
    }
    
    public static List<Map<String, Object>> rsToList(ResultSet r) throws SQLException
    {
        ResultSetMetaData meta = r.getMetaData();
        return rsToList(r, meta);
    }
    
    public static List<Map<String, Object>> rsToList(ResultSet r, ResultSetMetaData meta) throws SQLException
    {
        List<Map<String, Object>> allRows = new ArrayList<Map<String, Object>>();
        
        int colCount = meta.getColumnCount();
        while (r.next())
        {
            Map<String, Object> currRowProps = new HashMap<String, Object>();
            String rowKeyValue = null;
            for (int k = 1; k <= colCount; k++)
            {
                Object valObj = null;
                String clobStr = null;
                String propName = meta.getColumnLabel(k);
                int columnType = meta.getColumnType(k);
                if (columnType == Types.CLOB)
                {
                    Clob obj = r.getClob(k);
                    if (obj != null)
                    {
                        clobStr = convertClobToStr(obj);
                        valObj = clobStr;
                    }
                    else
                    {
                        clobStr = new String("");
                        valObj = clobStr;
                    }
                }
                else
                {
                    valObj = r.getObject(k);
                }
                if (null != valObj)
                {
                    currRowProps.put(propName, valObj);
                }
            }
            allRows.add(currRowProps);
        }
        return allRows;
    }
    
    private static String convertClobToStr(Clob obj)
    {
        
        Reader reader = null;
        try
        {
            reader = obj.getCharacterStream();
        }
        catch (SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        BufferedReader buffRead = new BufferedReader(reader);
        if (buffRead != null)
        {
            StringBuilder sb = new StringBuilder();
            String line = new String();
            try
            {
                while ((line = buffRead.readLine()) != null)
                {
                    sb.append(line).append("\n");
                }
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            return sb.toString();
        }
        else
        {
            
            return "";
            
        }
        
    }
    
    /**
     * example tuple: (ID, PROPKEY, PROPVALUE) call with: (1, 2, 3) colIdxs
     * returns a map of all rows, where the map lookup key is the value of the
     * 'ID' column and the value is a map of (propkey, propvalue) pairs.
     * 
     * @param r
     * @param keyColIdx
     * @param propKeyColIdx
     * @param propValColIdx
     * @return
     * @throws SQLException
     */
    public static Map<String, Map<String, String>> propKeyValueRSToMap(ResultSet r, int keyColIdx, int propKeyColIdx,
                                    int propValColIdx) throws SQLException
    {
        Map<String, Map<String, String>> allRows = new HashMap<String, Map<String, String>>();
        ResultSetMetaData meta = r.getMetaData();
        int colCount = meta.getColumnCount();
        
        while (r.next())
        {
            String rowKeyValue = null;
            
            Object keyValObj = r.getObject(keyColIdx);
            if (null != keyValObj)
            {
                rowKeyValue = "" + keyValObj;
                Map<String, String> existingRowProps = allRows.get(rowKeyValue);
                
                Map<String, String> currRowProps = getCurrRowProps(r, propKeyColIdx, propValColIdx);
                if (null != existingRowProps)
                {
                    existingRowProps.putAll(currRowProps);
                }
                else
                {
                    existingRowProps = currRowProps;
                    allRows.put(rowKeyValue, existingRowProps);
                }
            }
        }
        
        return allRows;
    }
    
    // make sure to do a r.next() before the first call.
    public static Map<String, String> getCurrRowProps(ResultSet currRs, int propKeyColIdx, int propValColIdx) throws SQLException
    {
        HashMap currRowProps = new HashMap<String, String>();
        String propKey = null;
        Object propKeyObj = currRs.getObject(propKeyColIdx);
        if (null != propKeyObj)
        {
            propKey = "" + propKeyObj;
            
            String propValue = null;
            Object propValObj = currRs.getObject(propValColIdx);
            if (null != propValObj)
            {
                propValue = "" + propValObj;
                currRowProps.put(propKey, propValue);
            }
        }
        return currRowProps;
    }
    
    public static List select(Connection conn, String selectSQL, String... whereValues) throws Throwable
    {
        List<String> whereValuesList = new ArrayList<String>();
        if (null != whereValues)
        {
            for (String currArg : whereValues)
            {
                whereValuesList.add(currArg);
            }
        }
        return select(conn, selectSQL, whereValuesList);
    }
    
    /**
     * returns a list of rows - where each row is a Map of columnName, value
     * pairs whereValues could be null or empty to indicate 'all' rows.
     * 
     * @param whereValues
     * @return
     * @throws Throwable
     */
    public static List select(Connection conn, String selectSQL, List<String> whereValues) throws Throwable
    {
        List listofNameValueMaps = new ArrayList();
        
        PreparedStatement stmt = prepSQL(conn, selectSQL, whereValues);
        ResultSet rs = stmt.executeQuery();
        
        boolean valid = rs.next();
        if (valid)
        {
            ResultSetMetaData rsm = rs.getMetaData();
            int numCols = rsm.getColumnCount();
            
            while (valid)
            {
                Map currRowMap = new HashMap();
                listofNameValueMaps.add(currRowMap);
                for (int columnIndex = 1; columnIndex <= numCols; columnIndex++)
                {
                    String currName = (String) rsm.getColumnLabel(columnIndex);
                    Object currValue = rs.getObject(columnIndex);
                    currRowMap.put(currName, currValue);
                }
                valid = rs.next();
            }
        }
        stmt.close();
        return listofNameValueMaps;
    }
    
    public static PreparedStatement prepSQL(Connection conn, String sql, Object... whereValues) throws Throwable
    {
        List<Object> whereValuesList = new ArrayList<Object>();
        if (null != whereValues)
        {
            for (Object currArg : whereValues)
            {
                whereValuesList.add(currArg);
            }
        }
        return prepSQL(conn, sql, whereValuesList);
    }
    
    public static PreparedStatement prepSQL(Connection conn, String sql, List whereValues) throws Throwable
    {
        PreparedStatement stmt = conn.prepareStatement(sql);
        ParameterMetaData pMD = stmt.getParameterMetaData();
        
        int pIdx = 0;
        for (Iterator whIter = whereValues.iterator(); whIter.hasNext();)
        {
            Object currWHVal = whIter.next();
            pIdx++;
            int jdbcType = pMD.getParameterType(pIdx);
            int scale = pMD.getScale(pIdx);
            setParamObject(stmt, pIdx, jdbcType, scale, currWHVal);
        }
        return stmt;
    }
    
    public static StringBuffer selectAsXML(Connection conn, String selectSQL, List<String> whereValues, String rowTag,
                                    String columnTag) throws Throwable
    {
        StringBuffer xml = null;
        PreparedStatement stmt = prepSQL(conn, selectSQL, whereValues);
        ResultSet rs = stmt.executeQuery();
        xml = resultSet2XML(rs, rowTag, columnTag);
        stmt.close();
        return xml;
    }
    
    /**
     * returns the number of rows deleted if whereValues is empty and there are
     * no '?' params in the sql, then all rows would be deleted
     * 
     * @param conn
     * @param deleteSQL
     * @param whereValues
     * @return
     * @throws Throwable
     */
    public static int delete(Connection conn, String deleteSQL, List<String> whereValues) throws Throwable
    {
        PreparedStatement stmt = prepSQL(conn, deleteSQL, whereValues);
        stmt.execute();
        int numDeleted = stmt.getUpdateCount();
        stmt.close();
        return numDeleted;
    }
    
    public static void execDDL(Connection conn, String[] ddls) throws Throwable
    {
        boolean currAC = conn.getAutoCommit();
        conn.setAutoCommit(false);
        
        Throwable t = null;
        
        for (int stmtIdx = 0; stmtIdx < ddls.length; stmtIdx++)
        {
            String currDDL = ddls[stmtIdx];
            try
            {
                execDDL(conn, currDDL);
            }
            catch (Throwable e)
            {
                t = e;
                conn.rollback();
                break;
            }
        }
        
        if (null != t)
        {
            conn.rollback();
        }
        else
        {
            conn.commit();
        }
        
        conn.setAutoCommit(currAC);
        if (null != t)
        {
            throw t;
        }
    }
    
    public static void execDDL(Connection conn, String ddl) throws Throwable
    {
        PreparedStatement stmt = conn.prepareStatement(ddl);
        stmt.execute();
        stmt.close();
    }
    
    public static void addObjectFromMap(PreparedStatement stmt, List restrictToPropertyNames, Map<String, Object> propertiesMap,
                                    boolean useBatch) throws Throwable
    {
        addObjectFromMap(stmt, restrictToPropertyNames, propertiesMap, null, useBatch);
    }
    
    public static void addObjectFromMap(PreparedStatement stmt, List restrictToPropertyNames, Map<String, Object> propertiesMap,
                                    List additionalValues, boolean useBatch) throws Throwable

    {
        int pIdx = 0;
        
        ParameterMetaData pMD = stmt.getParameterMetaData();
        
        for (Iterator propNamesIter = restrictToPropertyNames.iterator(); propNamesIter.hasNext();)
        {
            pIdx++;
            String currPName = (String) propNamesIter.next();
            Object currPvalue = propertiesMap.get(currPName);
            int jdbcType = pMD.getParameterType(pIdx);
            int scale = pMD.getScale(pIdx);
            setParamObject(stmt, pIdx, jdbcType, scale, currPvalue);
        }
        
        if (null != additionalValues)
        {
            for (Iterator addValuesIter = additionalValues.iterator(); addValuesIter.hasNext();)
            {
                pIdx++;
                Object currPvalue = (Object) addValuesIter.next();
                int jdbcType = pMD.getParameterType(pIdx);
                int scale = pMD.getScale(pIdx);
                setParamObject(stmt, pIdx, jdbcType, scale, currPvalue);
            }
        }
        
        if (useBatch)
        {
            stmt.addBatch();
        }
        else
        {
            stmt.execute();
        }
    }

    public static String convertByteArrayToString(byte[] bytes)
    {
        StringBuffer out = new StringBuffer();
        
        for (int i = 0; i < bytes.length; ++i)
        {
            short s = bytes[i];
            if (s < 0)
            {
                s = (short) (256 + s);
            }
            else if (s < 16)
            {
                out.append("0");
            }
            out.append(Integer.toHexString(s));
            
        }
        
        String stringRep = out.toString();
        return stringRep;
        
    }
    
    public static byte[] convertStringToByteArray(String str)
    {
        str = str.trim();
        int len = str.length();
        if (len % 2 != 0)
        {
            throw new NumberFormatException();
        }
        
        byte[] bytes = new byte[len / 2];
        
        for (int i = 0; i < str.length() / 2; ++i)
        {
            short b = Short.parseShort(str.substring(2 * i, 2 * i + 2), 16);
            byte thisByte = (byte) b;
            bytes[i] = thisByte;
        }
        return bytes;
    }

}
