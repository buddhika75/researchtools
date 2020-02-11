/*
 * The MIT License
 *
 * Copyright 2020 ari_soton_ac_uk.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lk.gov.health.phsp.bean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import lk.gov.health.phsp.entity.DataColumn;
import lk.gov.health.phsp.entity.DataRow;
import lk.gov.health.phsp.entity.DataSource;
import lk.gov.health.phsp.entity.DataValue;
import lk.gov.health.phsp.entity.Item;
import lk.gov.health.phsp.entity.Project;
import lk.gov.health.phsp.enums.DataType;
import lk.gov.health.phsp.enums.ItemType;
import lk.gov.health.phsp.facade.DataColumnFacade;
import lk.gov.health.phsp.facade.DataRowFacade;
import lk.gov.health.phsp.facade.DataSourceFacade;
import lk.gov.health.phsp.facade.DataValueFacade;
import lk.gov.health.phsp.facade.ProjectFacade;
import lk.gov.health.phsp.facade.util.JsfUtil;
import org.apache.commons.collections4.map.HashedMap;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author ari_soton_ac_uk
 */
@Named
@SessionScoped
public class DataMergeController implements Serializable {

    @EJB
    private ProjectFacade projectFacade;
    @EJB
    private DataSourceFacade dataSourceFacade;
    @EJB
    private DataColumnFacade dataColumnFacade;
    @EJB
    private DataRowFacade dataRowFacade;
    @EJB
    private DataValueFacade dataValueFacade;

    @Inject
    private WebUserController webUserController;

    private UploadedFile file;
    private Integer dataStartRow = 1;
    private Integer dataStartColumn = 0;
    private Integer dataHeaderRow = 0;
    private Integer dataEndRow = null;
    private Integer dataEndColumn = null;
    private Project selectedProject;
    private DataSource selectedDataSource;

    private List<DataSource> dataSourcesOfSelectedProject;
    private List<Project> myProjects;

    private List<DataColumn> dataColumnsMasterOfSelectedProject;

    private List<DataRow> dataRowsOfSelectedProject;
    private List<DataColumn> dataColumnsOfSelectedProject;
    private List<DataValue> dataValuesOfSelectedProject;
    private Map<String, DataValue> dataValuesMapOfSelectedProject;
    private List<ColumnModel> dataColumnModelssOfSelectedProject;

    private List<DataRow> dataRowsOfSelectedDataSource;
    private List<DataColumn> dataColumnsOfSelectedDataSource;
    private List<DataValue> dataValuesOfSelectedDataSource;
    private Map<String, DataValue> dataValuesMapOfSelectedDataSource;
    private List<ColumnModel> dataColumnModelssOfSelectedDataSource;

    /**
     * Creates a new instance of DataMerge
     */
    public DataMergeController() {
    }

    public String toViewSelectedProject() {
        if (selectedProject == null) {
            JsfUtil.addErrorMessage("Please select a project");
            return "";
        }
        fillDataSourcesOfSelectedProject();
        fillMasterDataColumnsOfSelectedProject();
        return "/dataMerge/project";
    }

    public String toViewSelectedDatasource() {
        if (selectedDataSource == null) {
            JsfUtil.addErrorMessage("No datasource");
            return "";
        }
        fillDataForSelectedDatasource();
        return "/dataMerge/data_source";
    }

    public String toViewSelectedDatasourceWithoutFillingData() {
        if (selectedDataSource == null) {
            JsfUtil.addErrorMessage("No datasource");
            return "";
        }
        return "/dataMerge/data_source";
    }

    public void fillDataForSelectedDatasource() {
        createColumnsForSelectedDataSource();
        createColumnModelForSelectedDataSource();
        createRowsForSelectedDataSource();
        fillDataValuesForSelectedDataSource();
        createColumnsForSelectedProject();
    }

    public void fillDataForSelectedProject() {
        createColumnsForSelectedProject();
        createColumnModelForSelectedProject();
        createRowsForSelectedProject();
        fillDataValuesForSelectedProject();
    }

    public String toUploadNewFile() {
        selectedDataSource = null;
        selectedProject = null;
        return "/dataMerge/upload_file";
    }

    public String toViewMyProjects() {
        String j = "select p from Project p "
                + " where p.retired=:ret "
                + " and p.institution=:ins"
                + " order by p.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("ins", webUserController.getLoggedUser().getInstitution());
        myProjects = getProjectFacade().findByJpql(j, m);
        return "/dataMerge/my_projects";
    }

    public String toAddANewDataSourceForTheProject() {
        selectedDataSource = new DataSource();
        return "/dataMerge/upload_file";
    }

    public String toComplieAllData() {
        if (selectedProject == null) {
            JsfUtil.addErrorMessage("Select a Project");
            return "";
        }
        fillDataForSelectedProject();
        return "/dataMerge/project_all_data";
    }

    public void fillDataSourcesOfSelectedProject() {
        String j = "select ds from DataSource ds "
                + " where ds.retired=:ret "
                + " and ds.project=:pro"
                + " order by ds.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("pro", selectedProject);
        dataSourcesOfSelectedProject = getDataSourceFacade().findByJpql(j, m);
    }

    public void fillMasterDataColumnsOfSelectedProject() {
        String j = "select ds from DataColumn ds "
                + " where ds.retired=:ret "
                + " and ds.project=:pro"
                + " order by ds.orderNo";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("pro", selectedProject);
        dataColumnsMasterOfSelectedProject = getDataColumnFacade().findByJpql(j, m);
    }

    public Project createAndSaveANewProject() {
        Project p = new Project();
        p.setInstitution(webUserController.getLoggedUser().getInstitution());
        p.setOwner(webUserController.getLoggedUser());
        p.setCreatedAt(new Date());
        p.setCreatedBy(webUserController.getLoggedUser());
        p.setName("Project By " + webUserController.getLoggedUser().getName());
        getProjectFacade().create(p);
        return p;
    }

    public DataSource createAndSaveANewDataSource(String fileName) {
        DataSource ds = new DataSource();
        ds.setProject(selectedProject);
        ds.setCreatedAt(new Date());
        ds.setCreatedBy(webUserController.getLoggedUser());
        ds.setName(fileName);
        ds.setFileName(fileName);
        getDataSourceFacade().create(ds);
        return ds;
    }

    public String uploadFileToMerge() {

        if (file == null) {
            JsfUtil.addErrorMessage("Error in Uploading file. No such file");
            return "";
        }

        if (file.getFileName() == null) {
            JsfUtil.addErrorMessage("Error in Uploading file. No such file name.");
            return "";
        }

        if (selectedProject == null) {
            selectedProject = createAndSaveANewProject();
        }

        if (selectedDataSource == null) {
            selectedDataSource = createAndSaveANewDataSource(file.getFileName());
        } else {
            selectedDataSource.setProject(selectedProject);
            selectedDataSource.setCreatedAt(new Date());
            selectedDataSource.setCreatedBy(webUserController.getLoggedUser());
            selectedDataSource.setName(file.getFileName());
            selectedDataSource.setFileName(file.getFileName());
            if (selectedDataSource.getId() == null) {
                getDataSourceFacade().create(selectedDataSource);
            } else {
                getDataSourceFacade().edit(selectedDataSource);
            }
        }

        try {
            String strParentCode;
            String strItemName;
            String strItemType;
            String strItemCode;

            Item parent = null;

            File inputWorkbook;
            Workbook w;
            Cell cell;
            InputStream in;

            JsfUtil.addSuccessMessage(file.getFileName());

            try {
                JsfUtil.addSuccessMessage(file.getFileName());
                in = file.getInputstream();
                File f;
                f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
                FileOutputStream out = new FileOutputStream(f);
                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = in.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
                in.close();
                out.flush();
                out.close();

                inputWorkbook = new File(f.getAbsolutePath());

                JsfUtil.addSuccessMessage("Excel File Opened");
                w = Workbook.getWorkbook(inputWorkbook);
                Sheet sheet = w.getSheet(0);

                if (dataStartRow == null) {
                    dataStartRow = 1;
                }
                if (dataStartColumn == null) {
                    dataStartColumn = 0;
                }
                if (dataEndRow == null) {
                    dataEndRow = sheet.getRows() - 1;
                }
                if (dataEndColumn == null) {
                    dataEndColumn = sheet.getColumns() - 1;
                }

                if (dataHeaderRow == null) {
                    dataHeaderRow = 0;
                }
                Map<Integer, DataColumn> columnsMap = new HashMap<Integer, DataColumn>();
                Map<Integer, DataRow> rowsMap = new HashMap<Integer, DataRow>();

                for (int myCol = dataStartColumn; myCol < (dataEndColumn + 1); myCol++) {
                    cell = sheet.getCell(myCol, dataHeaderRow);

                    DataColumn col = new DataColumn();
                    col.setOrderNo(myCol - dataStartColumn);
                    col.setDataSource(selectedDataSource);
                    col.setCreatedAt(new Date());
                    col.setCreatedBy(webUserController.getLoggedUser());
                    col.setName(cell.getContents());
                    getDataColumnFacade().create(col);

                    columnsMap.put(col.getOrderNo(), col);
                }

                for (int myRow = dataStartRow; myRow < (dataEndRow + 1); myRow++) {
                    DataRow row = new DataRow();

                    row.setCreatedAt(new Date());
                    row.setCreatedBy(webUserController.getLoggedUser());
                    row.setDataSource(selectedDataSource);
                    row.setOrderNo(myRow - dataStartRow);
                    getDataRowFacade().create(row);

                    rowsMap.put(row.getOrderNo(), row);
                }

                for (int myCol = dataStartColumn; myCol < (dataEndColumn + 1); myCol++) {

                    DataColumn col = columnsMap.get(myCol - dataStartColumn);

                    for (int myRow = dataStartRow; myRow < (dataEndRow + 1); myRow++) {
                        DataRow row = rowsMap.get(myRow - dataStartRow);

                        cell = sheet.getCell(myCol, myRow);

                        DataValue val = new DataValue();
                        val.setDataColumn(col);
                        val.setDataRow(row);
                        val.setUploadValue(cell.getContents());
                        val.setDataSource(selectedDataSource);
                        getDataValueFacade().create(val);

                    }
                }

                JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
                
                
                fillDataForSelectedDatasource();
                identifyDataTypesOfColumnsOfSelectedDataSource();
                
                updateProjectColumns();
                
                fillMasterDataColumnsOfSelectedProject();
                
                return toViewSelectedDatasourceWithoutFillingData();

            } catch (IOException ex) {
                JsfUtil.addErrorMessage(ex.getMessage());
                return "";
            } catch (BiffException e) {
                JsfUtil.addErrorMessage(e.getMessage());
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    public void updateProjectColumns() {
        System.out.println("update Project Columns");
        String j = "select c from DataColumn c "
                + " where c.retired=:ret "
                + " and c.project=:pro "
                + " order by c.orderNo";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("pro", selectedProject);
        List<DataColumn> pcs = getDataColumnFacade().findByJpql(j, m);
        System.out.println("pcs = " + pcs);
        System.out.println("dataColumnsOfSelectedDataSource = " + dataColumnsOfSelectedDataSource);
        for (DataColumn dsCol : dataColumnsOfSelectedDataSource) {
            boolean suitableColumnFound = false;
            boolean nameIsExactlyTheSame = false;
            boolean dataTypeIsSimilar = false;
            
            for (DataColumn pCol : pcs) {
                
                
                System.out.println("pCol = " + pCol);
                if (dsCol.getName().equalsIgnoreCase(pCol.getName())) {
                    nameIsExactlyTheSame = true;
                }
                if (dsCol.getDataType().equals(pCol.getDataType())) {
                    dataTypeIsSimilar = true;
                }

                System.out.println("nameIsExactlyTheSame = " + nameIsExactlyTheSame);
                System.out.println("dataTypeIsSimilar = " + dataTypeIsSimilar);
                
                //TODO: Add More Logic
                if (nameIsExactlyTheSame && dataTypeIsSimilar) {
                    suitableColumnFound = true;
                    dsCol.setReferance(pCol);
                    getDataColumnFacade().edit(dsCol);
                    break;
                }

            }
            
            System.out.println("suitableColumnFound = " + suitableColumnFound);

            if (!suitableColumnFound) {
                DataColumn newPCol = new DataColumn();
                newPCol.setDataType(dsCol.getDataType());
                newPCol.setCreatedAt(new Date());
                newPCol.setCreatedBy(webUserController.getLoggedUser());
                newPCol.setName(dsCol.getName());
                newPCol.setProject(selectedProject);
                getDataColumnFacade().create(newPCol);

                dsCol.setReferance(newPCol);
                getDataColumnFacade().edit(dsCol);
            }

        }

        j = "select c from DataColumn c "
                + " where c.retired=:ret "
                + " and c.project=:pro "
                + " order by c.orderNo";
        m = new HashMap();
        m.put("ret", false);
        m.put("pro", selectedProject);
        pcs = getDataColumnFacade().findByJpql(j, m);

        int count = 0;

        for (DataColumn pCol : pcs) {

            pCol.setOrderNo(count);
            getDataColumnFacade().edit(pCol);
            count ++;

        }

    }

    /**
     *
     *
     * To Do
     *
     * Check Date
     * http://regexlib.com/DisplayPatterns.aspx?cattabindex=4&categoryid=5&p=7
     *
     * Check Similarity in Names
     * https://stackoverflow.com/questions/955110/similarity-string-comparison-in-java
     *
     */
    public void identifyDataTypesOfColumnsOfSelectedDataSource() {
        if (selectedDataSource == null) {
            JsfUtil.addErrorMessage("No Datasource");
            return;
        }
        for (DataColumn c : dataColumnsOfSelectedDataSource) {
            List<DataValue> dvs = new ArrayList<>();
            for (DataRow r : dataRowsOfSelectedDataSource) {
                DataValue dv = dataValuesMapOfSelectedDataSource.get(c.getOrderNo() + "," + r.getOrderNo());
                if (!dv.getUploadValue().trim().equals("")) {
                    dvs.add(dv);
                }
            }
            boolean isInt = false;
            boolean isReal = false;
            boolean isDateTime = false;
            boolean isShortText = false;
            boolean isLongText = true;
            isInt = isInteger(dvs);
            if (isInt) {
                c.setDataType(DataType.Integer_Number);
            } else {
                isReal = isReal(dvs);
                if (isReal) {
                    c.setDataType(DataType.Real_Number);
                } else {
                    c.setDataType(DataType.Long_Text);
                }
            }
            getDataColumnFacade().edit(c);
        }

    }

    public boolean isInteger(List<DataValue> dvs) {
        for (DataValue s : dvs) {
            if (s.getUploadValue() != null) {
                boolean thisIsInteger = s.getUploadValue().matches("^-?\\d+$");
                if (!thisIsInteger) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isReal(List<DataValue> dvs) {
        for (DataValue s : dvs) {
            if (s.getUploadValue() != null) {
                boolean thisIsReal = s.getUploadValue().matches("^-?\\d+(\\.\\d+)?$");
                if (!thisIsReal) {
                    return false;
                }
            }
        }
        return true;
    }

    public void createColumnsForSelectedDataSource() {
        String j = "select c from DataColumn c "
                + " where c.dataSource=:dc "
                + " order by c.orderNo";
        Map m = new HashMap();
        m.put("dc", selectedDataSource);
        dataColumnsOfSelectedDataSource = getDataColumnFacade().findByJpql(j, m);
    }

    public void createColumnsForSelectedProject() {
        String j = "select c from DataColumn c "
                + " where c.project=:dc "
                + " order by c.orderNo";
        Map m = new HashMap();
        m.put("dc", selectedProject);
        dataColumnsOfSelectedProject = getDataColumnFacade().findByJpql(j, m);
    }

    public void createColumnModelForSelectedDataSource() {
        dataColumnModelssOfSelectedDataSource = new ArrayList<>();
        for (DataColumn dc : dataColumnsOfSelectedDataSource) {
            ColumnModel cm = new ColumnModel(dc.getName(), dc.getName());
            dataColumnModelssOfSelectedDataSource.add(cm);
        }
    }

    public void createColumnModelForSelectedProject() {
        dataColumnModelssOfSelectedProject = new ArrayList<>();
        for (DataColumn dc : dataColumnsOfSelectedProject) {
            ColumnModel cm = new ColumnModel(dc.getName(), dc.getName());
            dataColumnModelssOfSelectedProject.add(cm);
        }
    }

    public void createRowsForSelectedDataSource() {
        String j = "select c from DataRow c "
                + " where c.dataSource=:dc "
                + " order by c.orderNo";
        Map m = new HashMap();
        m.put("dc", selectedDataSource);
        dataRowsOfSelectedDataSource = getDataRowFacade().findByJpql(j, m);
    }

    public void createRowsForSelectedProject() {
        String j = "select c from DataRow c "
                + " where c.dataSource.project=:dc "
                + " order by c.dataSource.project.id, c.orderNo";
        Map m = new HashMap();
        m.put("dc", selectedProject);
        dataRowsOfSelectedProject = getDataRowFacade().findByJpql(j, m);
    }

    public void fillDataValuesForSelectedDataSource() {
        String j = "select c from DataValue c "
                + " where c.dataSource=:dc";
        Map m = new HashMap();
        m.put("dc", selectedDataSource);
        dataValuesOfSelectedDataSource = getDataValueFacade().findByJpql(j, m);
        dataValuesMapOfSelectedDataSource = new HashedMap<String, DataValue>();
        for (DataValue dv : dataValuesOfSelectedDataSource) {
            String cr = dv.getDataColumn().getOrderNo()
                    + ","
                    + dv.getDataRow().getOrderNo();
            dataValuesMapOfSelectedDataSource.put(cr, dv);
        }
    }

    public void fillDataValuesForSelectedProject() {

        Long l = 0l;
        dataValuesMapOfSelectedProject = new HashedMap<String, DataValue>();

        for (DataRow pdr : dataRowsOfSelectedProject) {

            String j = "select c from DataValue c "
                    + " where c.dataRow=:dc "
                    + " order by c.dataSource.project.id, c.dataRow.orderNo";
            Map m = new HashMap();
            m.put("dc", pdr);
            dataValuesOfSelectedProject = getDataValueFacade().findByJpql(j, m);

            for (DataValue dv : dataValuesOfSelectedProject) {
                if (dv.getDataColumn().getReferance() != null) {
                    String cr = dv.getDataColumn().getReferance().getOrderNo()
                            + ","
                            + l;
                    dataValuesMapOfSelectedProject.put(cr, dv);
                }

            }
            l++;

        }

    }

    public String uploadedValueOfSelectedDataSource(int col, int row) {

        String cr = col + "," + row;
        DataValue dv = dataValuesMapOfSelectedDataSource.get(cr);
        if (dv == null) {
            return "";
        }
        return dv.getUploadValue();
    }

    public String uploadedValueOfSelectedProject(int col, int row) {
        String cr = col + "," + row;
        DataValue dv = dataValuesMapOfSelectedProject.get(cr);
        if (dv == null) {
            return "";
        }

        return dv.getUploadValue();
    }

    public Project getSelectedProject() {
        return selectedProject;
    }

    public void setSelectedProject(Project selectedProject) {
        this.selectedProject = selectedProject;
    }

    public DataSource getSelectedDataSource() {
        return selectedDataSource;
    }

    public void setSelectedDataSource(DataSource selectedDataSource) {
        this.selectedDataSource = selectedDataSource;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public Integer getDataStartRow() {
        return dataStartRow;
    }

    public void setDataStartRow(Integer dataStartRow) {
        this.dataStartRow = dataStartRow;
    }

    public ProjectFacade getProjectFacade() {
        return projectFacade;
    }

    public void setProjectFacade(ProjectFacade projectFacade) {
        this.projectFacade = projectFacade;
    }

    public DataSourceFacade getDataSourceFacade() {
        return dataSourceFacade;
    }

    public void setDataSourceFacade(DataSourceFacade dataSourceFacade) {
        this.dataSourceFacade = dataSourceFacade;
    }

    public DataColumnFacade getDataColumnFacade() {
        return dataColumnFacade;
    }

    public void setDataColumnFacade(DataColumnFacade dataColumnFacade) {
        this.dataColumnFacade = dataColumnFacade;
    }

    public DataRowFacade getDataRowFacade() {
        return dataRowFacade;
    }

    public void setDataRowFacade(DataRowFacade dataRowFacade) {
        this.dataRowFacade = dataRowFacade;
    }

    public DataValueFacade getDataValueFacade() {
        return dataValueFacade;
    }

    public void setDataValueFacade(DataValueFacade dataValueFacade) {
        this.dataValueFacade = dataValueFacade;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public Integer getDataHeaderRow() {
        return dataHeaderRow;
    }

    public void setDataHeaderRow(Integer dataHeaderRow) {
        this.dataHeaderRow = dataHeaderRow;
    }

    public Integer getDataEndRow() {
        return dataEndRow;
    }

    public void setDataEndRow(Integer dataEndRow) {
        this.dataEndRow = dataEndRow;
    }

    public Integer getDataEndColumn() {
        return dataEndColumn;
    }

    public void setDataEndColumn(Integer dataEndColumn) {
        this.dataEndColumn = dataEndColumn;
    }

    public Integer getDataStartColumn() {
        return dataStartColumn;
    }

    public void setDataStartColumn(Integer dataStartColumn) {
        this.dataStartColumn = dataStartColumn;
    }

    public List<DataRow> getDataRowsOfSelectedDataSource() {
        return dataRowsOfSelectedDataSource;
    }

    public void setDataRowsOfSelectedDataSource(List<DataRow> dataRowsOfSelectedDataSource) {
        this.dataRowsOfSelectedDataSource = dataRowsOfSelectedDataSource;
    }

    public List<ColumnModel> getDataColumnModelssOfSelectedDataSource() {
        return dataColumnModelssOfSelectedDataSource;
    }

    public void setDataColumnModelssOfSelectedDataSource(List<ColumnModel> dataColumnModelssOfSelectedDataSource) {
        this.dataColumnModelssOfSelectedDataSource = dataColumnModelssOfSelectedDataSource;
    }

    public Map<String, DataValue> getDataValuesMapOfSelectedDataSource() {
        return dataValuesMapOfSelectedDataSource;
    }

    public void setDataValuesMapOfSelectedDataSource(Map<String, DataValue> dataValuesMapOfSelectedDataSource) {
        this.dataValuesMapOfSelectedDataSource = dataValuesMapOfSelectedDataSource;
    }

    public List<DataColumn> getDataColumnsOfSelectedDataSource() {
        return dataColumnsOfSelectedDataSource;
    }

    public void setDataColumnsOfSelectedDataSource(List<DataColumn> dataColumnsOfSelectedDataSource) {
        this.dataColumnsOfSelectedDataSource = dataColumnsOfSelectedDataSource;
    }

    public List<DataValue> getDataValuesOfSelectedDataSource() {
        return dataValuesOfSelectedDataSource;
    }

    public void setDataValuesOfSelectedDataSource(List<DataValue> dataValuesOfSelectedDataSource) {
        this.dataValuesOfSelectedDataSource = dataValuesOfSelectedDataSource;
    }

    public List<DataSource> getDataSourcesOfSelectedProject() {
        return dataSourcesOfSelectedProject;
    }

    public void setDataSourcesOfSelectedProject(List<DataSource> dataSourcesOfSelectedProject) {
        this.dataSourcesOfSelectedProject = dataSourcesOfSelectedProject;
    }

    public List<Project> getMyProjects() {
        return myProjects;
    }

    public void setMyProjects(List<Project> myProjects) {
        this.myProjects = myProjects;
    }

    public List<DataColumn> getDataColumnsMasterOfSelectedProject() {
        return dataColumnsMasterOfSelectedProject;
    }

    public void setDataColumnsMasterOfSelectedProject(List<DataColumn> dataColumnsMasterOfSelectedProject) {
        this.dataColumnsMasterOfSelectedProject = dataColumnsMasterOfSelectedProject;
    }

    public void updateDataColumn(DataColumn col) {
        getDataColumnFacade().edit(col);
    }

    public void updateDataColumnOfSelectedDataSource() {
        for (DataColumn col : dataColumnsOfSelectedDataSource) {
            getDataColumnFacade().edit(col);
        }
    }

    public void updateDataRow(DataRow row) {
        getDataRowFacade().edit(row);
    }

    public void updateDataValue(DataValue val) {
        getDataValueFacade().edit(val);
    }

    public void updateDataSource(DataSource ds) {
        getDataSourceFacade().edit(ds);
    }

    public void updateProject(Project pro) {
        getProjectFacade().edit(pro);
    }

    public List<DataRow> getDataRowsOfSelectedProject() {
        return dataRowsOfSelectedProject;
    }

    public void setDataRowsOfSelectedProject(List<DataRow> dataRowsOfSelectedProject) {
        this.dataRowsOfSelectedProject = dataRowsOfSelectedProject;
    }

    public List<DataColumn> getDataColumnsOfSelectedProject() {
        return dataColumnsOfSelectedProject;
    }

    public void setDataColumnsOfSelectedProject(List<DataColumn> dataColumnsOfSelectedProject) {
        this.dataColumnsOfSelectedProject = dataColumnsOfSelectedProject;
    }

    public List<DataValue> getDataValuesOfSelectedProject() {
        return dataValuesOfSelectedProject;
    }

    public void setDataValuesOfSelectedProject(List<DataValue> dataValuesOfSelectedProject) {
        this.dataValuesOfSelectedProject = dataValuesOfSelectedProject;
    }

    public Map<String, DataValue> getDataValuesMapOfSelectedProject() {
        return dataValuesMapOfSelectedProject;
    }

    public void setDataValuesMapOfSelectedProject(Map<String, DataValue> dataValuesMapOfSelectedProject) {
        this.dataValuesMapOfSelectedProject = dataValuesMapOfSelectedProject;
    }

    public List<ColumnModel> getDataColumnModelssOfSelectedProject() {
        return dataColumnModelssOfSelectedProject;
    }

    public void setDataColumnModelssOfSelectedProject(List<ColumnModel> dataColumnModelssOfSelectedProject) {
        this.dataColumnModelssOfSelectedProject = dataColumnModelssOfSelectedProject;
    }

    static public class ColumnModel implements Serializable {

        private String header;
        private String property;

        public ColumnModel(String header, String property) {
            this.header = header;
            this.property = property;
        }

        public String getHeader() {
            return header;
        }

        public String getProperty() {
            return property;
        }
    }

}
