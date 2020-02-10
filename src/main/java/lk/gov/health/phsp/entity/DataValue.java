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
package lk.gov.health.phsp.entity;

import java.io.Serializable;
import java.util.Date;
import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import lk.gov.health.phsp.bean.CommonController;
import lk.gov.health.phsp.entity.Item;
import lk.gov.health.phsp.enums.DataType;

/**
 *
 * @author ari_soton_ac_uk
 */
@Entity
public class DataValue implements Serializable {

    @Inject
    @Transient
    private CommonController commonController;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private DataColumn dataColumn;
    @ManyToOne
    private DataRow dataRow;

    @Enumerated(EnumType.STRING)
    private DataType dataType;
    @Lob
    private String uploadValue;
    private String valueString;
    @Lob
    private String valueStringLob;
    private Double valueDouble;
    private Long valueLong;
    @ManyToOne
    private Item valueItem;
    @Transient
    private String value;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date valueDateTime;
    private String dateTimeFormat;
    

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DataValue)) {
            return false;
        }
        DataValue other = (DataValue) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "lk.gov.health.phsp.bean.DataValue[ id=" + id + " ]";
    }

    public DataColumn getDataColumn() {
        return dataColumn;
    }

    public void setDataColumn(DataColumn dataColumn) {
        this.dataColumn = dataColumn;
    }

    public DataRow getDataRow() {
        return dataRow;
    }

    public void setDataRow(DataRow dataRow) {
        this.dataRow = dataRow;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public String getUploadValue() {
        return uploadValue;
    }

    public void setUploadValue(String uploadValue) {
        this.uploadValue = uploadValue;
    }

    public String getValueString() {
        return valueString;
    }

    public void setValueString(String valueString) {
        this.valueString = valueString;
    }

    public String getValueStringLob() {
        return valueStringLob;
    }

    public void setValueStringLob(String valueStringLob) {
        this.valueStringLob = valueStringLob;
    }

    public Double getValueDouble() {
        return valueDouble;
    }

    public void setValueDouble(Double valueDouble) {
        this.valueDouble = valueDouble;
    }

    public Long getValueLong() {
        return valueLong;
    }

    public void setValueLong(Long valueLong) {
        this.valueLong = valueLong;
    }

    public Item getValueItem() {
        return valueItem;
    }

    public void setValueItem(Item valueItem) {
        this.valueItem = valueItem;
    }
    
    

    public String getValue() {
        String value = null;
        switch (this.dataType) {
            case Boolean:
            case Byte_Array:
            case DateTime:
            case Integer_Number:
            case Item_Reference:
                if(valueItem!=null){
                    value = valueItem.getName();
                }
            case Long_Number:
                if (valueLong != null) {
                    value = commonController.longToString(valueLong);
                }
                break;
            case Real_Number:
                if (valueDouble != null) {
                    value = commonController.doubleToString(valueDouble);
                }
                break;
            case Long_Text:
                value = valueStringLob;
                break;
            case Short_Text:
                value = valueString;
                break;
        }
        if (this.value == null) {
            value = "";
        }
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public Date getValueDateTime() {
        return valueDateTime;
    }

    public void setValueDateTime(Date valueDateTime) {
        this.valueDateTime = valueDateTime;
    }

    public String getDateTimeFormat() {
        return dateTimeFormat;
    }

    public void setDateTimeFormat(String dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
    }

}
