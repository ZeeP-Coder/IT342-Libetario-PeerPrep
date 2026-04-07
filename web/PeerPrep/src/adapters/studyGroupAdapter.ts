import type { StudyGroup, StudyGroupDashboard, StudyPartner } from '../services/studyGroupService'

function normalizeText(value: string | undefined | null) {
  return typeof value === 'string' ? value.trim() : ''
}

export function adaptStudyPartner(partner: StudyPartner): StudyPartner {
  return {
    fullName: normalizeText(partner.fullName),
    email: normalizeText(partner.email),
    university: normalizeText(partner.university),
    major: normalizeText(partner.major),
    sharedGroups: Number.isFinite(partner.sharedGroups) ? partner.sharedGroups : 0,
  }
}

export function adaptStudyGroup(group: StudyGroup): StudyGroup {
  const currentMembers = Number.isFinite(group.currentMembers) ? group.currentMembers : 0
  const maxMembers = Number.isFinite(group.maxMembers) ? group.maxMembers : 0
  const joined = Boolean(group.joined)
  const joinable = currentMembers < maxMembers && !joined

  return {
    id: group.id,
    subject: normalizeText(group.subject),
    description: normalizeText(group.description),
    day: normalizeText(group.day),
    meetingTime: normalizeText(group.meetingTime),
    location: normalizeText(group.location),
    maxMembers,
    currentMembers,
    status: joinable ? 'Open' : 'Full',
    joined,
    ownedByCurrentUser: Boolean(group.ownedByCurrentUser),
    joinable,
    createdByName: normalizeText(group.createdByName),
    createdByEmail: normalizeText(group.createdByEmail),
    createdAt: normalizeText(group.createdAt),
    memberNames: Array.isArray(group.memberNames) ? group.memberNames : [],
  }
}

export function adaptStudyGroupDashboard(dashboard: StudyGroupDashboard): StudyGroupDashboard {
  return {
    currentUserName: normalizeText(dashboard.currentUserName),
    currentUserEmail: normalizeText(dashboard.currentUserEmail),
    activeGroups: Number.isFinite(dashboard.activeGroups) ? dashboard.activeGroups : 0,
    availableGroups: Number.isFinite(dashboard.availableGroups) ? dashboard.availableGroups : 0,
    myGroups: Number.isFinite(dashboard.myGroups) ? dashboard.myGroups : 0,
    partnerCount: Number.isFinite(dashboard.partnerCount) ? dashboard.partnerCount : 0,
    nextSession: dashboard.nextSession ? adaptStudyGroup(dashboard.nextSession) : null,
    availableStudyGroups: dashboard.availableStudyGroups.map(adaptStudyGroup),
    myStudyGroups: dashboard.myStudyGroups.map(adaptStudyGroup),
    studyPartners: dashboard.studyPartners.map(adaptStudyPartner),
  }
}
