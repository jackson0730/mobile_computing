from django.urls import path
from . import protocols

urlpatterns = [
    path('getLectures/', protocols.getLectures, name='getLectures'),
    path('checkin/', protocols.checkin, name='checkin'),
    path('askhelp/', protocols.askhelp, name='askhelp'),
    path('upload/', protocols.upload, name='upload'),
    path('help/', protocols.help, name='help'),
    path('check/', protocols.check, name='check'),
]