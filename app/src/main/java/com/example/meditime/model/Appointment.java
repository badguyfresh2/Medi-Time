package com.example.meditime.model;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.Date;

@IgnoreExtraProperties
public class Appointment {
    private String appointmentId, patientId, doctorId, patientName, doctorName;
    private String date, timeSlot, status, notes, paymentStatus, reason, specialization, hospital;
    private double consultationFeeUGX;
    private Long createdAt;

    /** Required no-arg constructor for RTDB deserialization */
    public Appointment() {}

    public String getAppointmentId()   { return appointmentId; }   public void setAppointmentId(String v)   { appointmentId = v; }
    public String getPatientId()       { return patientId; }       public void setPatientId(String v)       { patientId = v; }
    public String getDoctorId()        { return doctorId; }        public void setDoctorId(String v)        { doctorId = v; }
    public String getPatientName()     { return patientName; }     public void setPatientName(String v)     { patientName = v; }
    public String getDoctorName()      { return doctorName; }      public void setDoctorName(String v)      { doctorName = v; }
    public String getDate()            { return date; }            public void setDate(String v)            { date = v; }
    public String getTimeSlot()        { return timeSlot; }        public void setTimeSlot(String v)        { timeSlot = v; }
    public String getStatus()          { return status; }          public void setStatus(String v)          { status = v; }
    public String getNotes()           { return notes; }           public void setNotes(String v)           { notes = v; }
    public String getPaymentStatus()   { return paymentStatus; }   public void setPaymentStatus(String v)   { paymentStatus = v; }
    public String getReason()          { return reason; }          public void setReason(String v)          { reason = v; }
    public String getSpecialization()  { return specialization; }  public void setSpecialization(String v)  { specialization = v; }
    public String getHospital()        { return hospital; }        public void setHospital(String v)        { hospital = v; }
    public double getConsultationFeeUGX() { return consultationFeeUGX; } public void setConsultationFeeUGX(double v) { consultationFeeUGX = v; }
    public Long getCreatedAt()         { return createdAt; }       public void setCreatedAt(Long v)         { createdAt = v; }

    public long getCreatedAtLong() {
        return createdAt != null ? createdAt : 0L;
    }
}
