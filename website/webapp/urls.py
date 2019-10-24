from django.urls import path
from . import protocols
from django.conf.urls import url
from . import views

urlpatterns = [
    path('getLectures/', protocols.getLectures, name='getLectures'),
    path('checkin/', protocols.checkin, name='checkin'),
    path('askhelp/', protocols.askhelp, name='askhelp'),
    path('upload/', protocols.upload, name='upload'),
    path('help/', protocols.help, name='help'),
    path('check/', protocols.check, name='check'),
    path('webcheck/', protocols.webcheck, name='webcheck'),
    path('pushLink/', protocols.pushLink, name='pushLink'),
    path('selectastudent/', protocols.selectAStudent, name='selectAStudent'),

    
    # Please do not change 'index' to 'index/'
    # without '/' is for the webapp display
    path('index', views.index),
    path('register',views.register),
    path('login',views.login),
    path('account',views.account),
    path('attendance',views.attendance),
    path('',views.index),
    # with '/' is for the JsonResponse to mobileapp
    path('register/',protocols.register, name='register'),
    path('login/',protocols.login, name='login'),
]