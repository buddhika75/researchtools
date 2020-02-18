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

import java.io.FileOutputStream;
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
import lk.gov.health.phsp.entity.DataColumn;

import lk.gov.health.phsp.entity.DataSource;

import lk.gov.health.phsp.entity.Project;
import lk.gov.health.phsp.facade.DataColumnFacade;

import lk.gov.health.phsp.facade.DataSourceFacade;

import lk.gov.health.phsp.facade.ProjectFacade;
import lk.gov.health.phsp.facade.util.JsfUtil;
import org.primefaces.model.UploadedFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import lk.gov.health.phsp.pojcs.DataSourceFile;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author ari_soton_ac_uk
 */
@Named
@SessionScoped
public class DataMergeControllerXlsx implements Serializable {

    @EJB
    private ProjectFacade projectFacade;
    @EJB
    private DataSourceFacade dataSourceFacade;
    @EJB
    private DataColumnFacade dataColumnFacade;

    @Inject
    private WebUserController webUserController;

    private UploadedFile file;
    private Integer dataStartRow = 1;
    private Integer dataStartColumn = 0;
    private Integer dataHeaderRow = 0;
    private Integer dataEndRow = null;
    private Integer dataEndColumn = null;
    private Project selectedProject;
    private boolean createNewMasterCols = true;
    private DataSource selectedDataSource;

    private List<DataSource> dataSourcesOfSelectedProject;
    private List<Project> myProjects;

    private List<DataColumn> dataColumnsMasterOfSelectedProject;

    private List<DataColumn> dataColumnsOfSelectedProject;

    private List<ColumnModel> dataColumnModelssOfSelectedProject;

    private List<DataColumn> dataColumnsOfSelectedDataSource;

    private List<ColumnModel> dataColumnModelssOfSelectedDataSource;

    private DataSource removingDataSource;

    private DataColumn removingDataColumn;
    private Project removingProject;

    private String mergingMessage = null;
    private Long mergingCount;
    private Long mergingCol;
    private Long mergingRow;

    private StreamedContent downloafFile;

    /**
     * Creates a new instance of DataMerge
     */
    public DataMergeControllerXlsx() {
    }

    public String removeProject() {
        if (removingProject == null) {
            JsfUtil.addErrorMessage("Please Select");
            return "";
        }
        removingProject.setRetired(true);
        removingProject.setRetiredBy(webUserController.getLoggedUser());
        removingProject.setRetiredAt(new Date());
        getProjectFacade().edit(removingProject);
        removingProject = null;
        return toViewMyProjects();
    }

    public void removeDataColumn() {
        if (removingDataColumn == null) {
            JsfUtil.addErrorMessage("Please Select");
            return;
        }
        removingDataColumn.setRetired(true);
        removingDataColumn.setRetiredBy(webUserController.getLoggedUser());
        removingDataColumn.setRetiredAt(new Date());
        getDataColumnFacade().edit(removingDataColumn);
        removingDataColumn = null;
        fillMasterDataColumnsOfSelectedProject();
    }

    public void removeDataSource() {
        if (removingDataSource == null) {
            JsfUtil.addErrorMessage("Please Select");
            return;
        }
        removingDataSource.setRetired(true);
        removingDataSource.setRetiredBy(webUserController.getLoggedUser());
        removingDataSource.setRetiredAt(new Date());
        getDataSourceFacade().edit(removingDataSource);
        removingDataSource = null;
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

    public void addAColumnForSelectedProject() {
        DataColumn ndc = new DataColumn();
        ndc.setCreatedAt(new Date());
        ndc.setCreatedBy(webUserController.getLoggedUser());
        ndc.setProject(selectedProject);
        ndc.setName("New Column");
        ndc.setOrderNo(dataColumnsOfSelectedProject.size());
        getDataColumnFacade().create(ndc);
        fillMasterDataColumnsOfSelectedProject();
    }

    public String toViewSelectedDatasource() {
        if (selectedDataSource == null) {
            JsfUtil.addErrorMessage("No datasource");
            return "";
        }
        fillDataForSelectedDatasource();
        return "/dataMerge/data_source";
    }

    public void toToggleSelectionOfDatasource() {
        if (selectedDataSource == null) {
            JsfUtil.addErrorMessage("No datasource");
            return;
        }
        selectedDataSource.setSelected(!selectedDataSource.isSelected());
        getDataSourceFacade().edit(selectedDataSource);
    }

    public String toViewSelectedDatasourceWithoutFillingData() {
        if (selectedDataSource == null) {
            JsfUtil.addErrorMessage("No datasource");
            return "";
        }
        return "/dataMerge/data_source";
    }

    public void fillDataForSelectedDatasource() {
        System.out.println("mergingMessage = " + mergingMessage);
        mergingMessage = "Creating Columns";
        System.out.println("mergingMessage = " + mergingMessage);
        createColumnsForSelectedDataSource();
        mergingMessage = "Creating Column Models";
        System.out.println("mergingMessage = " + mergingMessage);
        createColumnModelForSelectedDataSource();
        mergingMessage = "Creating Master columns for Project";
        System.out.println("mergingMessage = " + mergingMessage);
        createColumnsForSelectedProject();
    }

    public String toUploadNewFile() {
        selectedDataSource = null;
        selectedProject = createAndSaveANewProject();
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
        mergingMessage = "";
        return "/dataMerge/my_projects";
    }

    public String toAddANewDataSourceForTheProject() {
        selectedDataSource = new DataSource();
        return "/dataMerge/upload_file";
    }

    public void downloadAllData() {
//        if (selectedProject == null) {
//            JsfUtil.addErrorMessage("Select a Project");
//            return;
//        }
//        createColumnsForSelectedProject();
//
//        Workbook wb = new XSSFWorkbook();
//
//        try (OutputStream fileOut = new FileOutputStream("/tmp/" + selectedProject.getName() + (new Date()).getTime() + ".xlsx")) {
//            wb.write(fileOut);
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(DataMergeControllerXlsx.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IOException ex) {
//            Logger.getLogger(DataMergeControllerXlsx.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        File newFile = new File("/tmp/" + selectedProject.getName() + (new Date()).getTime() + ".xlsx");
//
//        Sheet sheet = wb.createSheet(selectedProject.getName());
//
//        CreationHelper createHelper = wb.getCreationHelper();
//
//        int colNo = 0;
//        int rowNo = 0;
//        int writeStartRow = 1;
//
//        for (DataColumn pc : dataColumnsOfSelectedProject) {
//            Row row = sheet.createRow(0);
//            Cell cell = row.createCell(colNo);
//            cell.setCellValue(pc.getName());
//            colNo++;
//        }
//
//        rowNo = 1;
//
//        for (DataSource ds : dataSourcesOfSelectedProject) {
//
//            if (ds.isSelected()) {
//
//                mergingMessage = "Data Source - " + ds.getFileName();
//
//                System.out.println("Data Source - " + ds.getFileName());
//
//                DataSourceFile dsf = new DataSourceFile(ds);
//
//                Cell tc = dsf.getSheet().getCell(0, 0);
//
//                List<DataColumn> dataColumnsOfDataSource = findDataColumnsOfADataSource(ds);
//
//                int valueExtractCol = ds.getDataStartColumn();
//                int dsRow = 0;
//
//                for (DataColumn colOfDs : dataColumnsOfDataSource) {
//
//                    System.out.println("colOfDs.getName() = " + colOfDs.getName());
//
//                    if (colOfDs.getReferance() != null) {
//
//                        DataColumn cdOfP = colOfDs.getReferance();
//
//                        System.out.println("colOfDs.getReferance().getName() = " + colOfDs.getReferance().getName());
//
//                        mergingMessage += "<br/>" + "Column " + cdOfP.getName();
//
//                        dsRow = 0;
//                        for (int valueExtractRow = ds.getDataStartRow(); valueExtractRow < (ds.getDataEndRow() + 1); valueExtractRow++) {
//
//                            Row row = sheet.getRow(dsRow + writeStartRow);
//                            
//                            System.out.println("col:row = " + cdOfP.getOrderNo() + ":" + (dsRow + writeStartRow));
//
//                            Cell valueCell = dsf.getSheet().getCell(valueExtractCol, valueExtractRow);
//                            
//                            Row valueRow = 
//                            
//                            String valueString = valueCell.getContents();
//                            if (valueCell.getType() == CellType.LABEL) {
//                                Label labelCell = new Label(cdOfP.getOrderNo(), dsRow + writeStartRow, valueString);
//                                excelSheet.addCell(cdOfP.getOrderNo());
//                                
//                                Cell targetCell = row.getCell(0);
//                                
//                            } else if (valueCell.getType() == CellType.NUMBER) {
//                                NumberCell numCell = (NumberCell) valueCell;
//                                double num = numCell.getValue();
//                                Number numberCell = new Number(cdOfP.getOrderNo(), dsRow + writeStartRow, num);
//                                excelSheet.addCell(numberCell);
//                            } else if (valueCell.getType() == CellType.DATE) {
//                                DateCell dateCell = (DateCell) valueCell;
//                                Date date = dateCell.getDate();
//                                DateTime dateTimeCell = new DateTime(cdOfP.getOrderNo(), dsRow + writeStartRow, date);
//                                excelSheet.addCell(dateTimeCell);
//                            } else {
//                                Label label = new Label(cdOfP.getOrderNo(), dsRow + writeStartRow, valueString);
//                                excelSheet.addCell(label);
//                            }
//
//                            dsRow++;
//                        }
//                    }
//                    valueExtractCol++;
//                }
//                writeStartRow = writeStartRow + dsRow;
//                rowNo++;
//            }
//
//        }
//
//        OutputStream fileOut = new FileOutputStream("workbook.xls");
//
//        wb.write(fileOut);
//
//    



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

    public void writeFileToDataSource(File f, DataSource ds) {
        InputStream in;

        try {
            in = getFile().getInputstream();
            FileOutputStream out = new FileOutputStream(f);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            ds.setFileName(file.getFileName());
            ds.setFileType(file.getContentType());
            in = file.getInputstream();
            ds.setFileContents(IOUtils.toByteArray(in));
        } catch (IOException e) {
            System.out.println("Error " + e.getMessage());
        }

    }

    public String uploadFileToMerge() {

//        System.out.println("uploadFileToMerge");
//
//        mergingCount = 0l;
//        mergingMessage = "Uploading";
//
//        if (file == null) {
//            JsfUtil.addErrorMessage("Error in Uploading file. No such file");
//            return "";
//        }
//
//        if (file.getFileName() == null) {
//            JsfUtil.addErrorMessage("Error in Uploading file. No such file name.");
//            return "";
//        }
//
//        if (selectedProject == null) {
//            selectedProject = createAndSaveANewProject();
//        } else if (selectedProject.getId() == null) {
//            getProjectFacade().create(selectedProject);
//        } else {
//            getProjectFacade().edit(selectedProject);
//        }
//
//        if (selectedDataSource == null) {
//            selectedDataSource = createAndSaveANewDataSource(file.getFileName());
//        } else {
//            selectedDataSource.setProject(selectedProject);
//            selectedDataSource.setCreatedAt(new Date());
//            selectedDataSource.setCreatedBy(webUserController.getLoggedUser());
//            selectedDataSource.setName(file.getFileName());
//            selectedDataSource.setFileName(file.getFileName());
//            if (selectedDataSource.getId() == null) {
//                getDataSourceFacade().create(selectedDataSource);
//            } else {
//                getDataSourceFacade().edit(selectedDataSource);
//            }
//        }
//
//        try {
//
//            File inputWorkbook;
//            Workbook w;
//            Cell cell;
//            InputStream in;
//
//            try {
//
//                in = file.getInputstream();
//                File f;
//                f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
//                FileOutputStream out = new FileOutputStream(f);
//                int read = 0;
//                byte[] bytes = new byte[1024];
//                while ((read = in.read(bytes)) != -1) {
//                    out.write(bytes, 0, read);
//                }
//                in.close();
//                out.flush();
//                out.close();
//
//                inputWorkbook = new File(f.getAbsolutePath());
//
//                writeFileToDataSource(f, selectedDataSource);
//                getDataSourceFacade().edit(selectedDataSource);
//
//                JsfUtil.addSuccessMessage("Excel File Opened");
//                w = Workbook.getWorkbook(inputWorkbook);
//                Sheet sheet = w.getSheet(0);
//
//                if (dataStartRow == null) {
//                    dataStartRow = 1;
//                }
//                if (dataStartColumn == null) {
//                    dataStartColumn = 0;
//                }
//                if (dataEndRow == null) {
//                    dataEndRow = sheet.getRows() - 1;
//                }
//                if (dataEndColumn == null) {
//                    dataEndColumn = sheet.getColumns() - 1;
//                }
//
//                if (dataHeaderRow == null) {
//                    dataHeaderRow = 0;
//                }
//
//                selectedDataSource.setDataStartColumn(dataStartColumn);
//                selectedDataSource.setDataEndColumn(dataEndColumn);
//                selectedDataSource.setDataStartRow(dataStartRow);
//                selectedDataSource.setDataEndRow(dataEndRow);
//                selectedDataSource.setDataHeaderRow(dataHeaderRow);
//
//                getDataSourceFacade().edit(selectedDataSource);
//
//                for (int myCol = dataStartColumn; myCol < (dataEndColumn + 1); myCol++) {
//                    cell = sheet.getCell(myCol, dataHeaderRow);
//
//                    DataColumn col = new DataColumn();
//                    col.setOrderNo(myCol - dataStartColumn);
//                    col.setDataSource(selectedDataSource);
//                    col.setCreatedAt(new Date());
//                    col.setCreatedBy(webUserController.getLoggedUser());
//                    col.setName(cell.getContents());
//                    getDataColumnFacade().create(col);
//
//                    mergingCount++;
//                    mergingMessage = "Creating Columns - " + mergingCount;
//
//                    System.out.println("mergingMessage = " + mergingMessage);
//
//                }
//
//                mergingCount = 0l;
//
//                JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
//
//                fillDataForSelectedDatasource();
//
//                mergingMessage = "Updating Master columns for Project";
//                System.out.println("mergingMessage = " + mergingMessage);
//                updateProjectColumns();
//
//                mergingMessage = "FIlling Master columns for Project";
//                System.out.println("mergingMessage = " + mergingMessage);
//                fillMasterDataColumnsOfSelectedProject();
//
//                mergingMessage = "Resetting file date";
//                System.out.println("mergingMessage = " + mergingMessage);
//                resetUploadFileDate();
//
//                mergingMessage = "Finishing";
//                System.out.println("mergingMessage = " + mergingMessage);
//                return toViewSelectedDatasourceWithoutFillingData();
//
//            } catch (IOException ex) {
//                JsfUtil.addErrorMessage(ex.getMessage());
//                return "";
//            } catch (BiffException e) {
//                JsfUtil.addErrorMessage(e.getMessage());
//                return "";
//            }
//        } catch (Exception e) {
//            return "";
//        }
        return "";
    }

    public void resetUploadFileDate() {
        dataStartRow = 1;
        dataStartColumn = 0;
        dataHeaderRow = 0;
        dataEndRow = null;
        dataEndColumn = null;
        createNewMasterCols = true;
    }

    public List<DataColumn> findDataColumnsOfADataSource(DataSource ds) {
        String j = "select c from DataColumn c "
                + " where c.retired=:ret "
                + " and c.dataSource=:ds "
                + " order by c.orderNo";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("ds", ds);
        return getDataColumnFacade().findByJpql(j, m);
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

            for (DataColumn pCol : pcs) {

                System.out.println("pCol = " + pCol.getName());
                if (dsCol.getName().equalsIgnoreCase(pCol.getName())) {
                    nameIsExactlyTheSame = true;
                }

                System.out.println("nameIsExactlyTheSame = " + nameIsExactlyTheSame);
                System.out.println("dataTypeIsSimilar = ");

                //TODO: Add More Logic
                if (nameIsExactlyTheSame) {
                    suitableColumnFound = true;
                    dsCol.setReferance(pCol);
                    getDataColumnFacade().edit(dsCol);
                    break;
                }

            }

            System.out.println("suitableColumnFound = " + suitableColumnFound);

            if (createNewMasterCols) {

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
            count++;

        }

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
                + " and c.retired=:ret "
                + " and c.project.retired=:ret "
                + " order by c.orderNo";
        Map m = new HashMap();
        m.put("dc", selectedProject);
        m.put("ret", false);
        dataColumnsOfSelectedProject = getDataColumnFacade().findByJpql(j, m);
    }

    public void createColumnModelForSelectedDataSource() {
        dataColumnModelssOfSelectedDataSource = new ArrayList<>();
        for (DataColumn dc : dataColumnsOfSelectedDataSource) {
            ColumnModel cm = new ColumnModel(dc.getName(), dc.getName());
            dataColumnModelssOfSelectedDataSource.add(cm);
        }
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

    public List<ColumnModel> getDataColumnModelssOfSelectedDataSource() {
        return dataColumnModelssOfSelectedDataSource;
    }

    public void setDataColumnModelssOfSelectedDataSource(List<ColumnModel> dataColumnModelssOfSelectedDataSource) {
        this.dataColumnModelssOfSelectedDataSource = dataColumnModelssOfSelectedDataSource;
    }

    public List<DataColumn> getDataColumnsOfSelectedDataSource() {
        return dataColumnsOfSelectedDataSource;
    }

    public void setDataColumnsOfSelectedDataSource(List<DataColumn> dataColumnsOfSelectedDataSource) {
        this.dataColumnsOfSelectedDataSource = dataColumnsOfSelectedDataSource;
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

    public void updateDataSource(DataSource ds) {
        getDataSourceFacade().edit(ds);
    }

    public void updateProject(Project pro) {
        getProjectFacade().edit(pro);
    }

    public List<DataColumn> getDataColumnsOfSelectedProject() {
        return dataColumnsOfSelectedProject;
    }

    public void setDataColumnsOfSelectedProject(List<DataColumn> dataColumnsOfSelectedProject) {
        this.dataColumnsOfSelectedProject = dataColumnsOfSelectedProject;
    }

    public List<ColumnModel> getDataColumnModelssOfSelectedProject() {
        return dataColumnModelssOfSelectedProject;
    }

    public void setDataColumnModelssOfSelectedProject(List<ColumnModel> dataColumnModelssOfSelectedProject) {
        this.dataColumnModelssOfSelectedProject = dataColumnModelssOfSelectedProject;
    }

    public boolean isCreateNewMasterCols() {
        return createNewMasterCols;
    }

    public void setCreateNewMasterCols(boolean createNewMasterCols) {
        this.createNewMasterCols = createNewMasterCols;
    }

    public DataSource getRemovingDataSource() {
        return removingDataSource;
    }

    public void setRemovingDataSource(DataSource removingDataSource) {
        this.removingDataSource = removingDataSource;
    }

    public DataColumn getRemovingDataColumn() {
        return removingDataColumn;
    }

    public void setRemovingDataColumn(DataColumn removingDataColumn) {
        this.removingDataColumn = removingDataColumn;
    }

    public Project getRemovingProject() {
        return removingProject;
    }

    public void setRemovingProject(Project removingProject) {
        this.removingProject = removingProject;
    }

    public Project getSelectedProject() {
        return selectedProject;
    }

    public void setSelectedProject(Project selectedProject) {
        this.selectedProject = selectedProject;
    }

    public String getMergingMessage() {
        return mergingMessage;
    }

    public void setMergingMessage(String mergingMessage) {
        this.mergingMessage = mergingMessage;
    }

    public Long getMergingCount() {
        return mergingCount;
    }

    public void setMergingCount(Long mergingCount) {
        this.mergingCount = mergingCount;
    }

    public Long getMergingCol() {
        return mergingCol;
    }

    public void setMergingCol(Long mergingCol) {
        this.mergingCol = mergingCol;
    }

    public Long getMergingRow() {
        return mergingRow;
    }

    public void setMergingRow(Long mergingRow) {
        this.mergingRow = mergingRow;
    }

    public StreamedContent getDownloafFile() {
        return downloafFile;
    



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
