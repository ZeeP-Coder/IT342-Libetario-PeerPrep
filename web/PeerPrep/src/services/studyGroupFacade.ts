import { adaptStudyGroup, adaptStudyGroupDashboard } from '../adapters/studyGroupAdapter'
import {
  createStudyGroup,
  deleteStudyGroup,
  fetchStudyGroup,
  fetchStudyGroupDashboard,
  joinStudyGroup,
  leaveStudyGroup,
  type StudyGroup,
  type StudyGroupCreatePayload,
  type StudyGroupDashboard,
} from './studyGroupService'

type StudyGroupChangeListener = () => void

class StudyGroupFacade {
  private listeners = new Set<StudyGroupChangeListener>()

  subscribeToChanges(listener: StudyGroupChangeListener) {
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }

  private notifyChanges() {
    this.listeners.forEach((listener) => listener())
  }

  async loadDashboard(userEmail: string): Promise<StudyGroupDashboard> {
    const dashboard = await fetchStudyGroupDashboard(userEmail)
    return adaptStudyGroupDashboard(dashboard)
  }

  async loadGroup(groupId: number, userEmail: string): Promise<StudyGroup> {
    const group = await fetchStudyGroup(groupId, userEmail)
    return adaptStudyGroup(group)
  }

  async createGroup(payload: StudyGroupCreatePayload) {
    const response = await createStudyGroup(payload)
    this.notifyChanges()
    return response
  }

  async joinGroup(groupId: number, userEmail: string) {
    const response = await joinStudyGroup(groupId, { userEmail })
    this.notifyChanges()
    return response
  }

  async leaveGroup(groupId: number, userEmail: string) {
    const response = await leaveStudyGroup(groupId, { userEmail })
    this.notifyChanges()
    return response
  }

  async deleteGroup(groupId: number, userEmail: string) {
    const response = await deleteStudyGroup(groupId, userEmail)
    this.notifyChanges()
    return response
  }
}

export const studyGroupFacade = new StudyGroupFacade()
