package com.crm.service;

import com.crm.dto.request.ContactRequest;
import com.crm.entity.Contact;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final com.crm.util.AuthUtil authUtil;
    private final com.crm.repository.UserRepository userRepository;
    private final LeadService leadService;

    public List<Contact> getAllContacts(Long userId, String role) {
        if (authUtil.isSuperAdmin(role)) {
            return contactRepository.findAll();
        }
        com.crm.entity.User user = userRepository.findById(userId).orElse(null);
        String scopeMode = authUtil.resolveDataScopeMode(user, "CONTACTS");

        if ("ALL_DATA".equals(scopeMode)) {
            List<Long> companyUserIds = leadService.getCompanyUserIds(userId, role);
            return contactRepository.findByUserIdFkIn(companyUserIds);
        }
        if ("TEAM_DATA".equals(scopeMode)) {
            List<Long> teamUserIds = authUtil.getTeamLeadMemberUserIds(user);
            if (teamUserIds.isEmpty()) teamUserIds = List.of(-1L);
            List<Contact> contacts = contactRepository.findByUserIdFkIn(teamUserIds);
            if (contacts.isEmpty()) {
                List<Long> companyUserIds = leadService.getCompanyUserIds(userId, role);
                return contactRepository.findByUserIdFkIn(companyUserIds);
            }
            return contacts;
        }
        List<Contact> userContacts = contactRepository.findByUserIdFk(userId);
        if (userContacts.isEmpty()) {
            List<Long> companyUserIds = leadService.getCompanyUserIds(userId, role);
            return contactRepository.findByUserIdFkIn(companyUserIds);
        }
        return userContacts;
    }

    public Contact getById(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", id));
    }

    public Contact create(ContactRequest req, Long userId) {
        Contact contact = mapToEntity(req, new Contact());
        contact.setUserIdFk(userId);
        return contactRepository.save(contact);
    }

    public Contact update(Long id, ContactRequest req) {
        Contact contact = getById(id);
        return contactRepository.save(mapToEntity(req, contact));
    }

    public void delete(Long id) {
        contactRepository.delete(getById(id));
    }

    private Contact mapToEntity(ContactRequest req, Contact contact) {
        contact.setContactName(req.getContactName());
        contact.setContactMobileNo(req.getContactMobileNo());
        contact.setContactEmail(req.getContactEmail());
        contact.setContactAddress(req.getContactAddress());
        contact.setContactCity(req.getContactCity());
        contact.setContactState(req.getContactState());
        contact.setContactCountry(req.getContactCountry());
        contact.setContactOccasion(req.getContactOccasion());
        contact.setContactPostalCode(req.getContactPostalCode());
        contact.setContactOccasionDate(req.getContactOccasionDate());
        return contact;
    }
}
