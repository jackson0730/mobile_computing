from django.urls import path
from . import protocols

urlpatterns = [
    path('getLectures', protocols.getLectures, name='getLectures'),
    path('checkin', protocols.checkin, name='checkin'),
]