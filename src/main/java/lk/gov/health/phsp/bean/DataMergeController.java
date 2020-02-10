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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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
import lk.gov.health.phsp.enums.ItemType;
import lk.gov.health.phsp.facade.DataColumnFacade;
import lk.gov.health.phsp.facade.DataRowFacade;
import lk.gov.health.phsp.facade.DataSourceFacade;
import lk.gov.health.phsp.facade.DataValueFacade;
import lk.gov.health.phsp.facade.ProjectFacade;
import lk.gov.health.phsp.facade.util.JsfUtil;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author ari_soton_ac_uk
 */
@Named(value = "dataMerge")
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

    /**
     * Creates a new instance of DataMerge
     */
    public DataMergeController() {
    }

    public Project createAndSaveANewProject() {
        Project p = new Project();
        p.setInstitution(webUserController.getInstitution());
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
        getDataSourceFacade().create(ds);
        return ds;
    }

    public String importItemsFromExcel() {

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

        selectedDataSource = createAndSaveANewDataSource(file.getFileName());

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
                    col.setOrderNo(myCol);
                    col.setDataSource(selectedDataSource);
                    col.setCreatedAt(new Date());
                    col.setCreatedBy(webUserController.getLoggedUser());
                    col.setName(cell.getContents());
                    getDataColumnFacade().create(col);

                    columnsMap.put(myCol, col);
                }

                for (int myRow = dataStartRow; myRow < (dataEndRow + 1); myRow++) {
                    DataRow row = new DataRow();
                    row.setOrderNo(myRow);
                    row.setCreatedAt(new Date());
                    row.setCreatedBy(webUserController.getLoggedUser());
                    row.setDataSource(selectedDataSource);
                    row.setOrderNo(myRow);
                    getDataRowFacade().create(row);

                    rowsMap.put(myRow, row);
                }

                for (int myCol = dataStartColumn; myCol < (dataEndColumn + 1); myCol++) {

                    DataColumn col = columnsMap.get(myCol);

                    for (int myRow = dataStartRow; myRow < (dataEndRow + 1); myRow++) {
                        DataRow row = rowsMap.get(myRow);

                        cell = sheet.getCell(myCol, myRow);

                        DataValue val = new DataValue();
                        val.setDataColumn(col);
                        val.setDataRow(row);
                        val.setUploadValue(cell.getContents());
                        
                        getDataValueFacade().create(val);

                    }
                }

               

                JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
                return "";
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

}
