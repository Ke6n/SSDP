package edu.udo.cs.rvs.ssdp;

/**
 *  Eine Ziele der bekannte Dienst-List fuer User
 */
public class UserListLine {
    private String uuid = null;
    private String serviceType = null;

    public String getUuid() {
        return uuid;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setServiceType(String serverType) {
        this.serviceType = serverType;
    }

    /**
     *  UUID und Dienst-Typ laut gegebener Form einer bekannte Dienst-List-Zeile umformen
     *  @return eine umgeformt Dienst-List-Zeile
     */
    public String getUserListStyle(){
        return String.format("%s - %s", uuid, serviceType);
    }
}
