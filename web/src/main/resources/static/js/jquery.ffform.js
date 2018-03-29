/***************************************************************************************************************/

/* Plugin: jQuery Contact Form FFForm
/* Author: Muhammad Shahbaz Saleem
/* URL: http://www.egrappler.com/ffform-free-jquery-contact-form-plugin-with-validations-amazing-css3-animation
/* License: http://www.egrappler.com/license
/****************************************************************************************************************/

function sldiv(id){
    $("#id1").hide();
    $("#id2").hide();
    $("#"+id).show();
}

function sbmit(){

        var form = $("#form");
        var data = form.serialize();
        var url = 'wsdl';
        if(data){

        }
        $.ajax({
            type: 'POST',
            url: url,
            data: data,
            success: function (result) {

            }
        })

}