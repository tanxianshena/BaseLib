package com.tzh.baselib.view.pickerview

class PickerWeekDto(
    var week : Int ?= null,
    var type : Int ?= null,
){
    fun getWeekText() : String{
        return when(week){
            0->{
                 "星期日"
            }
            1->{
                "星期一"
            }
            2->{
                "星期二"
            }
            3->{
                "星期三"
            }
            4->{
                "星期四"
            }
            5->{
                "星期五"
            }
            6->{
                "星期六"
            }
            else->{
                "全部"
            }
        }
    }
}