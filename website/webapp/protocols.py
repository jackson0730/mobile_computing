from django.http import JsonResponse
from .models import *

def getLectures(request):
    lectures = Lecture.objects.all()

    if lectures.exists():
        response = {'status': True, 'lectures': []}
    else:
        response = {'status': False, 'lectures': []}

    for lecture in lectures:
        json = {
            'lectureID': lecture.ID,
            'latitude': lecture.latitude,
            'longitude': lecture.longitude,
            'dateTime': lecture.dateTime
        }

        response['lectures'].append(json)

    return JsonResponse(response)

def checkin(request):
    try:
        userID = request.POST['id']
        lectureID = request.POST['lectureID']

        user = User.objects.get(ID=userID)
        lecture = Lecture.objects.get(ID=lectureID)

        attendance = Attendance()
        attendance.userID = user
        attendance.lectureID = lecture
        attendance.save()

        response = {'status': True}
    except:
        response = {'status': False}

    return JsonResponse(response)