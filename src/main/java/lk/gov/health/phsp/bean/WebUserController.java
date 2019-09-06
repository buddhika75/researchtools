package lk.gov.health.phsp.bean;

import lk.gov.health.phsp.entity.Area;
import lk.gov.health.phsp.enums.AreaType;
import lk.gov.health.phsp.entity.WebUser;
import lk.gov.health.phsp.entity.Institution;
import lk.gov.health.phsp.enums.InstitutionType;
import lk.gov.health.phsp.entity.Item;
import lk.gov.health.phsp.enums.ItemType;
import lk.gov.health.phsp.entity.Client;
import lk.gov.health.phsp.entity.Encounter;
import lk.gov.health.phsp.entity.FormItem;
import lk.gov.health.phsp.entity.EncounterFormItem;
import lk.gov.health.phsp.entity.FormSet;
import lk.gov.health.phsp.entity.Form;
import lk.gov.health.phsp.enums.ProjectStageType;
import lk.gov.health.phsp.entity.Upload;
import lk.gov.health.phsp.enums.UploadType;
import lk.gov.health.phsp.enums.WebUserRole;
import lk.gov.health.phsp.facade.InstitutionFacade;
import lk.gov.health.phsp.facade.EncounterFacade;
import lk.gov.health.phsp.facade.ClientFacade;
import lk.gov.health.phsp.facade.ProjectInstitutionFacade;
import lk.gov.health.phsp.facade.ProjectSourceOfFundFacade;
import lk.gov.health.phsp.facade.UploadFacade;
import lk.gov.health.phsp.facade.WebUserFacade;
import lk.gov.health.phsp.facade.util.JsfUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.apache.commons.io.IOUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import org.primefaces.model.map.DefaultMapModel;
import org.primefaces.model.map.LatLng;
import org.primefaces.model.map.MapModel;
import org.primefaces.model.map.Marker;

@Named("webUserController")
@SessionScoped
public class WebUserController implements Serializable {

    /*
    EJBs
     */
    @EJB
    private lk.gov.health.phsp.facade.WebUserFacade ejbFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private ClientFacade projectFacade;
    @EJB
    private UploadFacade uploadFacade;
    @EJB
    private EncounterFacade projectAreaFacade;
    @EJB
    private ProjectInstitutionFacade projectInstitutionFacade;
    @EJB
    private ProjectSourceOfFundFacade projectSourceOfFundFacade;
    /*
    Controllers
     */
    @Inject
    private CommonController commonController;
    @Inject
    private AreaController areaController;
    @Inject
    private InstitutionController institutionController;
    @Inject
    private ItemController itemController;

    /*
    Variables
     */
    private List<WebUser> items = null;
    private List<Upload> currentProjectUploads;
    private List<Upload> clientUploads;
    private List<Upload> companyUploads;
    private List<Client> listOfProjects;

    private Area selectedProvince;
    private Area selectedDistrict;
    private Area selectedDsArea;
    private Area selectedGnArea;
    private Institution selectedLocation;
    private Item selectedSourceOfFund;
    private Double selectedFundValue;
    private Item selectedFundUnit;
    private String selectedFundComments;

    private FormSet removingProjectProvince;
    private FormItem removingProjectDistrict;
    private EncounterFormItem removingProjectInstitution;
    private Form removingProjectSourceOfFund;

    private List<Area> districtsAvailableForSelection;

    private List<Area> selectedDsAreas;
    private List<Area> selectedGnAreas;
    private Area[] selectedProvinces;

    private Encounter selectedProjectArea;

    private WebUser current;
    private Client currentProject;
    private Upload currentUpload;
    private Institution institution;

    private WebUser loggedUser;
    private String userName;
    private String password;
    private String passwordReenter;
    private MapModel emptyModel;

    private UploadedFile file;
    private String comments;

    private StreamedContent downloadingFile;

    private Date fromDate;
    private Date toDate;

    private Integer year;
    private Area province;
    private Area district;
    private Institution location;
    private Boolean allIslandProjects;
    private String searchKeyword;

    private String loginRequestResponse;

    private String locale;

    private ProjectStageType projectStageWorkingOn;
    private String projectStageWorkingOnComments;
    private Date projectStageWorkingOnDate;
    private String projectStageWorkingOnTitle;
    private String projectStageWorkingOnButtonTitle;
    private String projectStageWorkingOnDateTitle;
    private String projectStageWorkingOnCommentTitle;
    private String projectStageWorkingOnPeriodTitle;

    @PostConstruct
    public void init() {
        emptyModel = new DefaultMapModel();
    }

    public List<Area> getAreas(AreaType areaType, List<Area> superAreas) {
        String j;
        Map m = new HashMap();
        j = "select a "
                + " from Area a "
                + " where a.name is not null ";
        if (areaType != null) {
            j += " and a.type=:t";
            m.put("t", areaType);
        }
        if (superAreas != null) {
            j += " and a.parentArea in :pa ";
            m.put("pa", superAreas);
        }
        j += " order by a.name";
        System.out.println("m = " + m);
        System.out.println("j = " + j);
        List<Area> areas = getFacade().findBySQL(j, m);
        System.out.println("areas = " + areas);
        return areas;
    }

    public String toChangeMyDetails() {
        if (loggedUser == null) {
            return "";
        }
        current = loggedUser;
        return "/change_my_details";
    }

    public String toChangeMyPassword() {
        if (loggedUser == null) {
            return "";
        }
        password = "";
        passwordReenter = "";
        current = loggedUser;
        return "/change_my_password";
    }

   
    public void markLocationOnMap() {
        emptyModel = new DefaultMapModel();
        if (current == null) {
            return;
        }
        LatLng coord1 = new LatLng(current.getInstitution().getCoordinate().getLatitude(), current.getInstitution().getCoordinate().getLongitude());
        emptyModel.addOverlay(new Marker(coord1, current.getInstitution().getAddress()));
    }

    public void markLocationOnMapForBidders() {
        emptyModel = new DefaultMapModel();
        if (current == null) {
            return;
        }
        LatLng coord1 = new LatLng(current.getInstitution().getCoordinate().getLatitude(), current.getInstitution().getCoordinate().getLongitude());
        emptyModel.addOverlay(new Marker(coord1, current.getInstitution().getAddress()));
    }

    public String viewMedia() {
        if (currentUpload == null) {
            JsfUtil.addErrorMessage("Nothing is selected to view");
            return "";
        }
        if (currentUpload.getFileType().contains("image")) {
            return "/view_image";
        } else if (currentUpload.getFileType().contains("pdf")) {
            return "/view_pdf";
        } else {
            JsfUtil.addErrorMessage("NOT an image of a pdf file. ");
            return "";
        }
    }

 

    public void downloadCurrentFile() {
        if (currentUpload == null) {
            return;
        }
        InputStream stream = new ByteArrayInputStream(currentUpload.getBaImage());
        downloadingFile = new DefaultStreamedContent(stream, currentUpload.getFileType(), currentUpload.getFileName());
    }

    public StreamedContent getDownloadingFile() {
        downloadCurrentFile();
        return downloadingFile;
    }

    public String addMarker() {
        Marker marker = new Marker(new LatLng(current.getInstitution().getCoordinate().getLatitude(), current.getInstitution().getCoordinate().getLongitude()), current.getName());
        emptyModel.addOverlay(marker);
        getInstitutionFacade().edit(getCurrent().getInstitution());
        JsfUtil.addSuccessMessage("Location Recorded");
        return "";
    }

  
 

   
   



    /**
     *
     *
     *
     * Other Functions
     *
     *
     */
    public void uploadFiles() {
        if (getCurrentProject() == null) {
            lk.gov.health.phsp.facade.util.JsfUtil.addErrorMessage("No Project");
            return;
        }

        InputStream in;

        Upload u = new Upload();

        u.setProject(currentProject);
        u.setComments(comments);
        u.setCreatedAt(new Date());
        u.setUploadType(UploadType.Client_Upload_Prior_To_Proposal);
        currentProjectUploads = null;
        clientUploads = null;
        companyUploads = null;

        getUploadFacade().create(u);
        comments = "";

        StringWriter writer = new StringWriter();
        if (file != null) {
            try {
                in = getFile().getInputstream();
                File f = new File("P" + currentProject.getId() + "U" + u.getId());
                FileOutputStream out = new FileOutputStream(f);

                //            OutputStream out = new FileOutputStream(new File(fileName));
                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = in.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
                in.close();
                out.flush();
                out.close();

                u.setRetireComments(f.getAbsolutePath());
                u.setFileName(file.getFileName());
                u.setFileType(file.getContentType());
                in = file.getInputstream();
                u.setBaImage(IOUtils.toByteArray(in));
                getUploadFacade().edit(u);
                JsfUtil.addSuccessMessage("File Uploaded");
            } catch (IOException io) {
                System.out.println("io = " + io);
                JsfUtil.addErrorMessage("Error in Uploading. " + io.getMessage());
            } catch (Exception e) {
                System.out.println("e = " + e);
                JsfUtil.addErrorMessage("Error in Uploading. " + e.getMessage());
            }

        }
    }

    public Client getCurrentProject() {
        return currentProject;
    }

    public void setCurrentProject(Client currentProject) {
        this.currentProject = currentProject;
    }


    public String updateWebUserAndToMarkLocation() {
        try {
            getFacade().edit(current);
        } catch (Exception e) {
            System.out.println("e = " + e);
            JsfUtil.addErrorMessage("Username already taken. Please enter a different username");
            return "";
        }
        Institution ins = current.getInstitution();
        if (ins == null) {
            ins = new Institution();
            ins.setName(current.getName());
            ins.setEmail(current.getEmail());
            ins.setPhone(current.getTelNo());
            ins.setAddress(current.getPerson().getAddress());
            ins.setInstitutionType(InstitutionType.Regional_Department_of_Health_Department);
            getInstitutionFacade().create(ins);
            current.setInstitution(ins);
        } else {
            ins.setName(current.getName());
            ins.setEmail(current.getEmail());
            ins.setPhone(current.getTelNo());
            ins.setAddress(current.getPerson().getAddress());
            ins.setInstitutionType(InstitutionType.Regional_Department_of_Health_Department);
            getInstitutionFacade().edit(ins);
        }
        getFacade().edit(current);
        JsfUtil.addSuccessMessage("Your Details Updated. Please add Your Location Details.");
        return "/location_of_a_client";
    }

    public String registerUser() {
        if (!current.getWebUserPassword().equals(password)) {
            JsfUtil.addErrorMessage("Passwords are not matching. Please retry.");
            return "";
        }
        current.setWebUserRole(WebUserRole.Institution_User);
        try {
            getFacade().create(current);
        } catch (Exception e) {
            System.out.println("e = " + e);
            JsfUtil.addErrorMessage("Username already taken. Please enter a different username");
            return "";
        }

        setLoggedUser(current);
        JsfUtil.addSuccessMessage("Your Details Added as an institution user. Please contact us for changes");
        return "/index";
    }

    public String logOut() {
        loggedUser = null;
        return "/index";
    }

    public String toLogin() {
        return "/login";
    }

    public String login() {
        if (userName == null || userName.trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a Username");
            return "";
        }
        if (password == null || password.trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter the Password");
            return "";
        }
        if (!isFirstVisit()) {
            if (!checkLogin()) {
                JsfUtil.addErrorMessage("Username/Password Error. Please retry.");
                return "";
            }
        }
        JsfUtil.addSuccessMessage("Successfully Logged");
        return "index";
    }

    public String loginForMobile() {
        loginRequestResponse = "";
        if (userName == null || userName.trim().equals("")) {
            loginRequestResponse += "Wrong Isername. Please go back to settings and update.";
            return "/mobile/login_failure";
        }
        if (password == null || password.trim().equals("")) {
            loginRequestResponse += "Wrong Isername. Please go back to settings and update.";
            return "/mobile/login_failure";
        }
        if (!checkLogin()) {
            loginRequestResponse += "Wrong Isername. Please go back to settings and update.";
            return "/mobile/login_failure";
        }
        return "/mobile/index";
    }

    private boolean checkLogin() {
        System.out.println("Check Login");
        String temSQL;
        temSQL = "SELECT u FROM WebUser u WHERE lower(u.name)=:userName and u.retired =:ret";
        Map m = new HashMap();
        m.put("userName", userName.trim().toLowerCase());
        m.put("ret", false);
        loggedUser = getFacade().findFirstBySQL(temSQL, m);
        if (loggedUser == null) {
            return false;
        }
        if (password.equals(loggedUser.getWebUserPassword())) {
            System.out.println("Correct");
            return true;
        } else {
            System.out.println("wrong");
            loggedUser = null;
            return false;
        }

    }

    private boolean isFirstVisit() {
        System.out.println("is First Visit Check " + this);
        if (getFacade().count() <= 0) {
            JsfUtil.addSuccessMessage("First Visit");
            Institution ins = new Institution();
            ins.setName("Solar Bid, Inc");
            ins.setInstitutionType(InstitutionType.Ministry_of_Health);
            getInstitutionFacade().create(ins);
            WebUser wu = new WebUser();
            wu.getPerson().setName(userName);
            wu.setName(userName);
            wu.setWebUserPassword(password);
            wu.setInstitution(ins);
            wu.setWebUserRole(WebUserRole.System_Administrator);
            getFacade().create(wu);
            loggedUser = wu;
            return true;
        } else {
            System.out.println("NOT First Visit");
            return false;
        }

    }

    public String importProjectsFromExcel() {
        String strYear;
        String strProvince;
        String strFileNumber;
        String strDistrict;
        String strLocation;
        String strTile;
        String strDiscription;
        String strCost;
        String strFundSource;

        Double dblCost;
        Integer intYear;
        Institution insLocation;
        Area areaProvince;
        Area areaDistrict;
        Item itemFundSource;

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;

        int startRow = 1;

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

            for (int i = startRow; i < sheet.getRows(); i++) {

                Client np = new Client();
//                np.setCreatedAt(new Date());
//                np.setCreater(loggedUser);
//                np.setCurrentStageType(ProjectStageType.Awaiting_PEC_Approval);

                Map m = new HashMap();

                //Year
                cell = sheet.getCell(0, i);
                strYear = cell.getContents();
                try {
                    intYear = Integer.parseInt(strYear);
//                    np.setProjectYear(intYear);
                } catch (Exception e) {
                    System.out.println("e = " + i + " in " + e);
                }

                cell = sheet.getCell(1, i);
                strProvince = cell.getContents();

                cell = sheet.getCell(3, i);
                strDistrict = cell.getContents();

                areaProvince = areaController.getArea(strProvince, AreaType.Province, true, null);

                areaDistrict = areaController.getArea(strDistrict, AreaType.District, true, areaProvince);

//                np.setProvince(areaProvince);
//                np.setDistrict(areaDistrict);

                cell = sheet.getCell(2, i);
                strFileNumber = cell.getContents();
//                np.setFileNumber(strFileNumber);

                cell = sheet.getCell(4, i);
                strLocation = cell.getContents();
                insLocation = institutionController.getInstitution(strLocation, InstitutionType.Other, true);
//                np.setProjectLocation(insLocation);

                cell = sheet.getCell(5, i);
                strTile = cell.getContents();
//                np.setProjectTitle(strTile);

                cell = sheet.getCell(6, i);
                strDiscription = cell.getContents();
//                np.setProjectDescription(strDiscription);

                cell = sheet.getCell(7, i);
                strCost = cell.getContents();
                try {
                    dblCost = Double.parseDouble(strCost);
//                    np.setProjectCost(dblCost);
                } catch (Exception e) {
                    System.out.println(i + ". e = " + e);
                }

                cell = sheet.getCell(8, i);
                strFundSource = cell.getContents();
                itemFundSource = itemController.getItem(strFundSource, ItemType.Other, true);
//                np.setSourceOfFunds(itemFundSource);

                getProjectFacade().create(np);
                System.out.println("Added SUccessfully = " + i);

//                if (np.getProvince() != null) {
//                    FormSet pp = new FormSet();
//                    pp.setProject(np);
//                    pp.setArea(np.getProvince());
//                    getProjectAreaFacade().create(pp);
//                    np.getProjectProvinces().add(pp);
//                }
//                if (np.getDistrict() != null) {
//                    FormItem pp = new FormItem();
//                    pp.setProject(np);
//                    pp.setArea(np.getDistrict());
//                    getProjectAreaFacade().create(pp);
//                    np.getProjectDistricts().add(pp);
//                }
//                if (np.getProjectLocation() != null) {
//                    EncounterFormItem pp = new EncounterFormItem();
//                    pp.setProject(np);
//                    pp.setInstitution(np.getProjectLocation());
//                    getProjectInstitutionFacade().create(pp);
//                    np.getProjectLocations().add(pp);
//                }
//                if (np.getSourceOfFunds() != null) {
//                    Form pp = new Form();
//                    pp.setProject(np);
//                    pp.setSourceOfFund(np.getSourceOfFunds());
//                    getProjectSourceOfFundFacade().create(pp);
//                    np.getSourcesOfFunds().add(pp);
//                }

                getProjectFacade().edit(np);

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
    }

    public List<Upload> getUploads(Client p) {
        return getUploads(p, null);
    }

    public List<Upload> getUploads(Client p, UploadType type) {
        String j = "select u from Upload u "
                + " where u.project=:p ";
        Map m = new HashMap();
        m.put("p", currentProject);
        if (type != null) {
            j += " and u.uploadType=:t ";
            m.put("t", type);
        }
        return getUploadFacade().findBySQL(j, m);

    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public WebUserController() {
    }

    public WebUser getSelected() {
        return current;
    }

    private WebUserFacade getFacade() {
        return ejbFacade;
    }

    public String prepareList() {
        recreateModel();
        return "manage_users";
    }

    public String prepareView() {
        return "/webUser/View";
    }

    public String prepareCreate() {
        current = new WebUser();
        password = "";
        passwordReenter = "";
        return "/webUser/Create";
        //970224568

    }

    public String create() {
        if (!password.equals(current.getWebUserPassword())) {
            JsfUtil.addErrorMessage("Passwords do NOT match");
            return "";
        }
        try {
            getFacade().create(current);
            JsfUtil.addSuccessMessage(("WebUserCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ("PersistenceErrorOccured"));
            return "";
        }
        return prepareCreate();
    }

    public String prepareEdit() {
        return "Edit";
    }

    public String prepareEditPassword() {
        return "Password";
    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(("Updated"));
            return "manage_users";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, e.getMessage());
            return null;
        }
    }

    public String updateMyDetails() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(("Updated"));
            return "index";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, e.getMessage());
            return null;
        }
    }

    public String updateMyPassword() {
        current = loggedUser;
        if (current == null) {
            JsfUtil.addSuccessMessage(("Error. No Logged User"));
            return "";
        }

        if (!password.equals(passwordReenter)) {
            JsfUtil.addSuccessMessage(("Password Mismatch."));
            return "";
        }
        current.setWebUserPassword(password);
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(("Updated"));
            password = "";
            passwordReenter = "";
            return "/index";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, e.getMessage());
            return "";
        }
    }

    public void updateLoggedUser() {
        if (loggedUser == null) {
            return;
        }
        try {
            getFacade().edit(loggedUser);
            JsfUtil.addSuccessMessage(("Updated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, e.getMessage());
        }
    }

    public String updatePassword() {
        if (!password.equals(current.getWebUserPassword())) {
            JsfUtil.addErrorMessage("Passwords do NOT match");
            return "";
        }
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(("WebUserUpdated"));
            return "manage_users";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        performDestroy();
        recreateModel();
        return "manage_users";
    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(("WebUserDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ("PersistenceErrorOccured"));
        }
    }

    public List<WebUser> getItems() {
        if (items == null) {
            items = getFacade().findAll();
        }
        return items;
    }

    private void recreateModel() {
        items = null;
    }

    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    public WebUser getWebUser(java.lang.Long id) {
        return ejbFacade.find(id);
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public WebUser getLoggedUser() {
        return loggedUser;
    }

    public void setLoggedUser(WebUser loggedUser) {
        this.loggedUser = loggedUser;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public lk.gov.health.phsp.facade.WebUserFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(lk.gov.health.phsp.facade.WebUserFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public WebUser getCurrent() {
        return current;
    }

    public void setCurrent(WebUser current) {
        this.current = current;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public MapModel getEmptyModel() {
        return emptyModel;
    }

    public void setEmptyModel(MapModel emptyModel) {
        this.emptyModel = emptyModel;
    }

    public ClientFacade getProjectFacade() {
        return projectFacade;
    }

    public void setProjectFacade(ClientFacade projectFacade) {
        this.projectFacade = projectFacade;
    }

    public UploadFacade getUploadFacade() {
        return uploadFacade;
    }

    public void setUploadFacade(UploadFacade uploadFacade) {
        this.uploadFacade = uploadFacade;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Upload getCurrentUpload() {
        return currentUpload;
    }

    public void setCurrentUpload(Upload currentUpload) {
        this.currentUpload = currentUpload;
    }

    public List<Upload> getCurrentProjectUploads() {
        if (currentProjectUploads == null) {
            currentProjectUploads = getUploads(currentProject);
        }
        return currentProjectUploads;
    }

    public void setCurrentProjectUploads(List<Upload> currentProjectUploads) {
        this.currentProjectUploads = currentProjectUploads;
    }

    public List<Client> getListOfProjects() {
        return listOfProjects;
    }

    public void setListOfProjects(List<Client> listOfProjects) {
        this.listOfProjects = listOfProjects;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.MONTH, 0);
            c.set(Calendar.DATE, 1);
            fromDate = c.getTime();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = new Date();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<Upload> getClientUploads() {
        if (clientUploads == null) {
            clientUploads = getUploads(currentProject, UploadType.Client_Upload_Prior_To_Proposal);
        }
        return clientUploads;
    }

    public void setClientUploads(List<Upload> clientUploads) {
        this.clientUploads = clientUploads;
    }

    public List<Upload> getCompanyUploads() {
        if (companyUploads == null) {
            companyUploads = getUploads(currentProject, UploadType.Company_Design_Upload);
        }
        return companyUploads;
    }

    public void setCompanyUploads(List<Upload> companyUploads) {
        this.companyUploads = companyUploads;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Area[] getSelectedProvinces() {
        return selectedProvinces;
    }

    public void setSelectedProvinces(Area[] selectedProvinces) {
        this.selectedProvinces = selectedProvinces;
    }

    public Encounter getSelectedProjectArea() {
        return selectedProjectArea;
    }

    public void setSelectedProjectArea(Encounter selectedProjectArea) {
        this.selectedProjectArea = selectedProjectArea;
    }

    public EncounterFacade getProjectAreaFacade() {
        return projectAreaFacade;
    }

  

    public void setDistrictsAvailableForSelection(List<Area> districtsAvailableForSelection) {
        this.districtsAvailableForSelection = districtsAvailableForSelection;
    }

    public List<Area> getSelectedDsAreas() {
        if (selectedDsAreas == null) {
            selectedDsAreas = new ArrayList<>();
        }
        return selectedDsAreas;
    }

    public void setSelectedDsAreas(List<Area> selectedDsAreas) {
        this.selectedDsAreas = selectedDsAreas;
    }

    public List<Area> getSelectedGnAreas() {
        return selectedGnAreas;
    }

    public void setSelectedGnAreas(List<Area> selectedGnAreas) {
        if (selectedGnAreas == null) {
            selectedGnAreas = new ArrayList<>();
        }
        this.selectedGnAreas = selectedGnAreas;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Area getProvince() {
        return province;
    }

    public void setProvince(Area province) {
        this.province = province;
    }

    public Area getDistrict() {
        return district;
    }

    public void setDistrict(Area district) {
        this.district = district;
    }

    public Institution getLocation() {
        return location;
    }

    public void setLocation(Institution location) {
        this.location = location;
    }

    public Boolean getAllIslandProjects() {
        return allIslandProjects;
    }

    public void setAllIslandProjects(Boolean allIslandProjects) {
        this.allIslandProjects = allIslandProjects;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public String getLoginRequestResponse() {
        return loginRequestResponse;
    }

    public void setLoginRequestResponse(String loginRequestResponse) {
        this.loginRequestResponse = loginRequestResponse;
    }

    public String getLocale() {
        if (loggedUser != null) {
            locale = loggedUser.getDefLocale();
        } else {
            locale = "en";
        }
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public AreaController getAreaController() {
        return areaController;
    }

    public InstitutionController getInstitutionController() {
        return institutionController;
    }

    public ItemController getItemController() {
        return itemController;
    }

    public String getPasswordReenter() {
        return passwordReenter;
    }

    public void setPasswordReenter(String passwordReenter) {
        this.passwordReenter = passwordReenter;
    }

    public Area getSelectedProvince() {
        return selectedProvince;
    }

    public void setSelectedProvince(Area selectedProvince) {
        this.selectedProvince = selectedProvince;
    }

    public Area getSelectedDistrict() {
        return selectedDistrict;
    }

    public void setSelectedDistrict(Area selectedDistrict) {
        this.selectedDistrict = selectedDistrict;
    }

    public Area getSelectedDsArea() {
        return selectedDsArea;
    }

    public void setSelectedDsArea(Area selectedDsArea) {
        this.selectedDsArea = selectedDsArea;
    }

    public Area getSelectedGnArea() {
        return selectedGnArea;
    }

    public void setSelectedGnArea(Area selectedGnArea) {
        this.selectedGnArea = selectedGnArea;
    }

    public Institution getSelectedLocation() {
        return selectedLocation;
    }

    public void setSelectedLocation(Institution selectedLocation) {
        this.selectedLocation = selectedLocation;
    }

    public Item getSelectedSourceOfFund() {
        return selectedSourceOfFund;
    }

    public void setSelectedSourceOfFund(Item selectedSourceOfFund) {
        this.selectedSourceOfFund = selectedSourceOfFund;
    }

    public Double getSelectedFundValue() {
        return selectedFundValue;
    }

    public void setSelectedFundValue(Double selectedFundValue) {
        this.selectedFundValue = selectedFundValue;
    }

    public Item getSelectedFundUnit() {
        return selectedFundUnit;
    }

    public void setSelectedFundUnit(Item selectedFundUnit) {
        this.selectedFundUnit = selectedFundUnit;
    }

    public String getSelectedFundComments() {
        return selectedFundComments;
    }

    public void setSelectedFundComments(String selectedFundComments) {
        this.selectedFundComments = selectedFundComments;
    }

    public ProjectSourceOfFundFacade getProjectSourceOfFundFacade() {
        return projectSourceOfFundFacade;
    }

    public ProjectInstitutionFacade getProjectInstitutionFacade() {
        return projectInstitutionFacade;
    }

    public FormSet getRemovingProjectProvince() {
        return removingProjectProvince;
    }

    public void setRemovingProjectProvince(FormSet removingProjectProvince) {
        this.removingProjectProvince = removingProjectProvince;
    }

    public FormItem getRemovingProjectDistrict() {
        return removingProjectDistrict;
    }

    public void setRemovingProjectDistrict(FormItem removingProjectDistrict) {
        this.removingProjectDistrict = removingProjectDistrict;
    }

    public EncounterFormItem getRemovingProjectInstitution() {
        return removingProjectInstitution;
    }

    public void setRemovingProjectInstitution(EncounterFormItem removingProjectInstitution) {
        this.removingProjectInstitution = removingProjectInstitution;
    }

    public Form getRemovingProjectSourceOfFund() {
        return removingProjectSourceOfFund;
    }

    public void setRemovingProjectSourceOfFund(Form removingProjectSourceOfFund) {
        this.removingProjectSourceOfFund = removingProjectSourceOfFund;
    }

    public ProjectStageType getProjectStageWorkingOn() {
        return projectStageWorkingOn;
    }

    public void setProjectStageWorkingOn(ProjectStageType projectStageWorkingOn) {
        this.projectStageWorkingOn = projectStageWorkingOn;
    }

    public String getProjectStageWorkingOnComments() {
        return projectStageWorkingOnComments;
    }

    public void setProjectStageWorkingOnComments(String projectStageWorkingOnComments) {
        this.projectStageWorkingOnComments = projectStageWorkingOnComments;
    }

    public Date getProjectStageWorkingOnDate() {
        return projectStageWorkingOnDate;
    }

    public void setProjectStageWorkingOnDate(Date projectStageWorkingOnDate) {
        this.projectStageWorkingOnDate = projectStageWorkingOnDate;
    }

   
    @FacesConverter(forClass = WebUser.class)
    public static class WebUserControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            WebUserController controller = (WebUserController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "webUserController");
            return controller.getWebUser(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof WebUser) {
                WebUser o = (WebUser) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + WebUser.class.getName());
            }
        }

    }

}