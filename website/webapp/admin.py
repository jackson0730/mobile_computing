from django.contrib import admin
from .models import *
# Register your models here.
admin.site.register(User)
admin.site.register(Lecture)
admin.site.register(Attendance)
admin.site.register(PictureRequest)
admin.site.register(QuestionRequest)
admin.site.register(ChosenStudent)
admin.site.register(Link)
admin.site.register(RjectedPictureRequest)