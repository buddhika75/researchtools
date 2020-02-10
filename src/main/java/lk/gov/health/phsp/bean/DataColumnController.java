package lk.gov.health.phsp.bean;

import lk.gov.health.phsp.entity.DataColumn;
import lk.gov.health.phsp.facade.DataColumnFacade;

import java.io.Serializable;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import lk.gov.health.phsp.facade.util.JsfUtil;
import lk.gov.health.phsp.facade.util.JsfUtil.PersistAction;

@Named("dataColumnController")
@SessionScoped
public class DataColumnController implements Serializable {

    @EJB
    private lk.gov.health.phsp.facade.DataColumnFacade ejbFacade;
    private List<DataColumn> items = null;
    private DataColumn selected;

    public DataColumnController() {
    }

    public DataColumn getSelected() {
        return selected;
    }

    public void setSelected(DataColumn selected) {
        this.selected = selected;
    }

    protected void setEmbeddableKeys() {
    }

    protected void initializeEmbeddableKey() {
    }

    private DataColumnFacade getFacade() {
        return ejbFacade;
    }

    public DataColumn prepareCreate() {
        selected = new DataColumn();
        initializeEmbeddableKey();
        return selected;
    }

    public void create() {
        persist(PersistAction.CREATE, ResourceBundle.getBundle("/BundleRt1").getString("DataColumnCreated"));
        if (!JsfUtil.isValidationFailed()) {
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public void update() {
        persist(PersistAction.UPDATE, ResourceBundle.getBundle("/BundleRt1").getString("DataColumnUpdated"));
    }

    public void destroy() {
        persist(PersistAction.DELETE, ResourceBundle.getBundle("/BundleRt1").getString("DataColumnDeleted"));
        if (!JsfUtil.isValidationFailed()) {
            selected = null; // Remove selection
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public List<DataColumn> getItems() {
        if (items == null) {
            items = getFacade().findAll();
        }
        return items;
    }

    private void persist(PersistAction persistAction, String successMessage) {
        if (selected != null) {
            setEmbeddableKeys();
            try {
                if (persistAction != PersistAction.DELETE) {
                    getFacade().edit(selected);
                } else {
                    getFacade().remove(selected);
                }
                JsfUtil.addSuccessMessage(successMessage);
            } catch (EJBException ex) {
                String msg = "";
                Throwable cause = ex.getCause();
                if (cause != null) {
                    msg = cause.getLocalizedMessage();
                }
                if (msg.length() > 0) {
                    JsfUtil.addErrorMessage(msg);
                } else {
                    JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/BundleRt1").getString("PersistenceErrorOccured"));
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/BundleRt1").getString("PersistenceErrorOccured"));
            }
        }
    }

    public DataColumn getDataColumn(java.lang.Long id) {
        return getFacade().find(id);
    }

    public List<DataColumn> getItemsAvailableSelectMany() {
        return getFacade().findAll();
    }

    public List<DataColumn> getItemsAvailableSelectOne() {
        return getFacade().findAll();
    }

    @FacesConverter(forClass = DataColumn.class)
    public static class DataColumnControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DataColumnController controller = (DataColumnController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "dataColumnController");
            return controller.getDataColumn(getKey(value));
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
            if (object instanceof DataColumn) {
                DataColumn o = (DataColumn) object;
                return getStringKey(o.getId());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "object {0} is of type {1}; expected type: {2}", new Object[]{object, object.getClass().getName(), DataColumn.class.getName()});
                return null;
            }
        }

    }

}
